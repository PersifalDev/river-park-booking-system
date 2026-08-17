package ru.haritonenko.bookingservice.tasks.domain.async.processor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ru.haritonenko.bookingservice.domain.db.entity.BookingEntity;
import ru.haritonenko.bookingservice.domain.exception.BookingAvailabilityException;
import ru.haritonenko.bookingservice.domain.exception.BookingHoldFailedException;
import ru.haritonenko.bookingservice.domain.exception.BookingNotFoundException;
import ru.haritonenko.bookingservice.domain.exception.IllegalBookingStateException;
import ru.haritonenko.bookingservice.domain.service.BookingInventoryService;
import ru.haritonenko.bookingservice.domain.service.price.BookingPricingService;
import ru.haritonenko.bookingservice.tasks.domain.async.db.entity.AsyncBookingTaskEntity;
import ru.haritonenko.bookingservice.tasks.domain.async.db.repository.AsyncBookingTaskEntityRepository;
import ru.haritonenko.bookingservice.tasks.domain.async.dispatcher.config.AsyncBookingTaskDispatcherProperties;
import ru.haritonenko.bookingservice.tasks.domain.async.service.BookingTaskStateService;
import ru.haritonenko.bookingservice.tasks.domain.async.status.ProcessingStep;
import ru.haritonenko.bookingservice.tasks.domain.async.status.TaskExecutionStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class AsyncBookingTaskProcessor {

    private final ExecutorService externalHttpThreadPool;
    private final BookingTaskStateService bookingTaskStateService;
    private final BookingInventoryService bookingInventoryService;
    private final BookingPricingService bookingPricingService;
    private final AsyncBookingTaskEntityRepository taskRepository;
    private final TransactionTemplate transactionTemplate;
    private final AsyncBookingTaskDispatcherProperties properties;

    public AsyncBookingTaskProcessor(
            @Qualifier("externalHttpThreadPool") ExecutorService externalHttpThreadPool,
            BookingTaskStateService bookingTaskStateService,
            BookingInventoryService bookingInventoryService,
            BookingPricingService bookingPricingService,
            AsyncBookingTaskEntityRepository taskRepository,
            TransactionTemplate transactionTemplate,
            AsyncBookingTaskDispatcherProperties properties
    ) {
        this.externalHttpThreadPool = externalHttpThreadPool;
        this.bookingTaskStateService = bookingTaskStateService;
        this.bookingInventoryService = bookingInventoryService;
        this.bookingPricingService = bookingPricingService;
        this.taskRepository = taskRepository;
        this.transactionTemplate = transactionTemplate;
        this.properties = properties;
    }

    public TaskExecutionStatus processTask(AsyncBookingTaskEntity task) {
        logCurrentProcessingStep(task);

        var bookingId = task.getBookingId();
        ProcessingStep stepAtStart = task.getProcessingStep();

        if (!bookingTaskStateService.existsBookingById(bookingId)) {
            log.warn("Booking not found for task processing: bookingId={}, taskId={}", bookingId, task.getId());
            return TaskExecutionStatus.NON_RETRYABLE_ERROR;
        }

        if (stepAtStart == ProcessingStep.VALIDATE_REQUEST) {
            transactionTemplate.executeWithoutResult(status -> {
                BookingEntity booking = bookingTaskStateService.findBookingEntity(bookingId);
                log.info("Validating booking request: bookingId={}, taskId={}", bookingId, task.getId());

                if (!booking.getCheckOutDate().isAfter(booking.getCheckInDate())) {
                    bookingTaskStateService.markBookingFailed(bookingId, "Check out date must be after check in date");

                    throw new IllegalBookingStateException(
                            "Booking request validation failed id=%s".formatted(bookingId)
                    );
                }

                task.setProcessingStep(ProcessingStep.CHECK_AVAILABILITY);
                taskRepository.save(task);
                log.info("Booking request validation finished: bookingId={}, taskId={}, nextStep={}",
                        bookingId,
                        task.getId(),
                        task.getProcessingStep()
                );
            });
            stepAtStart = ProcessingStep.CHECK_AVAILABILITY;
            logCurrentProcessingStep(task);
        }

        try {
            if (stepAtStart == ProcessingStep.CHECK_AVAILABILITY) {
                CompletableFuture<Void> eventChain = CompletableFuture
                        .supplyAsync(() -> {
                            BookingEntity booking = findBookingEntity(task);
                            log.info("Checking booking availability: bookingId={}, taskId={}", bookingId, task.getId());
                            return bookingInventoryService.isAvailable(booking);
                        }, externalHttpThreadPool)
                        .thenApplyAsync(available -> transactionTemplate.execute(status -> {
                            if (!Boolean.TRUE.equals(available)) {
                                log.warn("Booking availability check failed: bookingId={}, taskId={}", bookingId, task.getId());
                                bookingTaskStateService.markBookingFailed(bookingId, "No available rooms for requested period");
                                throw new BookingAvailabilityException(
                                        "No available rooms for bookingId=%s".formatted(bookingId)
                                );
                            }

                            task.setProcessingStep(ProcessingStep.CALCULATE_PRICE);
                            taskRepository.save(task);
                            log.info("Booking availability check succeeded: bookingId={}, taskId={}, nextStep={}",
                                    bookingId,
                                    task.getId(),
                                    task.getProcessingStep()
                            );
                            return null;
                        }), externalHttpThreadPool)
                        .thenCompose(ignore -> getCalculatePriceFuture(task))
                        .thenCompose(price -> getCreateHoldFuture(task, price))
                        .thenCompose(ignore -> getSaveBookingFuture(task))
                        .handleAsync((res, ex) -> {
                            if (ex == null) {
                                return res;
                            }
                            throw new CompletionException(unwrap(ex));
                        }, externalHttpThreadPool);

                eventChain.get();
                log.info("Booking task processed successfully from availability event chain: bookingId={}, taskId={}",
                        bookingId,
                        task.getId()
                );
                return TaskExecutionStatus.SUCCESS;
            }

            if (stepAtStart == ProcessingStep.CALCULATE_PRICE) {
                getCalculatePriceFuture(task)
                        .thenCompose(price -> getCreateHoldFuture(task, price))
                        .thenCompose(ignore -> getSaveBookingFuture(task))
                        .get();
                log.info("Booking task processed successfully from calculate price step: bookingId={}, taskId={}",
                        bookingId,
                        task.getId()
                );
                return TaskExecutionStatus.SUCCESS;
            }

            if (stepAtStart == ProcessingStep.CREATE_HOLD) {
                BigDecimal priceAmount = findBookingEntity(task).getPriceAmount();
                getCreateHoldFuture(task, priceAmount)
                        .thenCompose(ignore -> getSaveBookingFuture(task))
                        .get();
                log.info("Booking task processed successfully from create hold step: bookingId={}, taskId={}",
                        bookingId,
                        task.getId()
                );
                return TaskExecutionStatus.SUCCESS;
            }

            if (stepAtStart == ProcessingStep.SAVE_BOOKING) {
                getSaveBookingFuture(task).get();
                log.info("Booking task processed successfully from save booking step: bookingId={}, taskId={}",
                        bookingId,
                        task.getId()
                );
                return TaskExecutionStatus.SUCCESS;
            }

            log.warn("Unsupported processing step: bookingId={}, taskId={}, step={}",
                    bookingId,
                    task.getId(),
                    stepAtStart
            );
            return TaskExecutionStatus.RETRYABLE_ERROR;
        } catch (InterruptedException ex) {
            log.warn("Booking task processing interrupted: bookingId={}, taskId={}", bookingId, task.getId(), ex);
            Thread.currentThread().interrupt();
            return TaskExecutionStatus.RETRYABLE_ERROR;
        } catch (ExecutionException ex) {
            Throwable exception = unwrap(ex);
            log.warn("Exception while processing booking task: bookingId={}, taskId={}", bookingId, task.getId(), exception);

            if (exception instanceof BookingAvailabilityException
                    || exception instanceof BookingHoldFailedException
                    || exception instanceof IllegalBookingStateException) {
                return TaskExecutionStatus.NON_RETRYABLE_ERROR;
            }
            return TaskExecutionStatus.RETRYABLE_ERROR;
        }
    }

    private CompletableFuture<BigDecimal> getCalculatePriceFuture(AsyncBookingTaskEntity task) {
        return CompletableFuture
                .supplyAsync(() -> {
                    BookingEntity booking = findBookingEntity(task);
                    log.info("Calculating booking price: bookingId={}, taskId={}", task.getBookingId(), task.getId());
                    return bookingPricingService.calculatePrice(booking);
                }, externalHttpThreadPool)
                .orTimeout(externalCallTimeoutMillis(), TimeUnit.MILLISECONDS)
                .thenApplyAsync(priceAmount -> transactionTemplate.execute(status -> {
                    BookingEntity booking = bookingTaskStateService.findBookingEntity(task.getBookingId());
                    bookingTaskStateService.updateBookingPrice(booking.getId(), priceAmount);
                    task.setProcessingStep(ProcessingStep.CREATE_HOLD);
                    taskRepository.save(task);
                    log.info("Booking price calculated: bookingId={}, taskId={}, price={}, nextStep={}",
                            task.getBookingId(),
                            task.getId(),
                            priceAmount,
                            task.getProcessingStep()
                    );
                    return priceAmount;
                }), externalHttpThreadPool)
                .handleAsync((result, ex) -> {
                    if (ex == null) {
                        return result;
                    }
                    throw new CompletionException(unwrap(ex));
                }, externalHttpThreadPool);
    }

    private CompletableFuture<Void> getCreateHoldFuture(AsyncBookingTaskEntity task, BigDecimal priceAmount) {
        return CompletableFuture
                .supplyAsync(() -> {
                    BookingEntity booking = findBookingEntity(task);
                    return bookingInventoryService.getTotalUnitsFromRoomCategory(booking.getRoomCategoryId());
                }, externalHttpThreadPool)
                .orTimeout(externalCallTimeoutMillis(), TimeUnit.MILLISECONDS)
                .thenAcceptAsync(totalUnits -> transactionTemplate.executeWithoutResult(status -> {
                    try {
                        BookingEntity booking = bookingTaskStateService.findBookingEntity(task.getBookingId());
                        log.info("Creating booking hold: bookingId={}, taskId={}", task.getBookingId(), task.getId());
                        bookingInventoryService.holdInventory(booking, totalUnits);
                        bookingTaskStateService.setBookingHold(
                                task.getBookingId(),
                                priceAmount,
                                OffsetDateTime.now().plus(properties.getHoldTtl())
                        );
                        task.setProcessingStep(ProcessingStep.SAVE_BOOKING);
                        taskRepository.save(task);
                        log.info("Booking hold created successfully: bookingId={}, taskId={}, nextStep={}",
                                task.getBookingId(),
                                task.getId(),
                                task.getProcessingStep()
                        );
                    } catch (BookingAvailabilityException ex) {
                        log.warn("Booking hold creation failed: bookingId={}, taskId={}", task.getBookingId(), task.getId(), ex);
                        bookingTaskStateService.markBookingFailed(task.getBookingId(), ex.getMessage());
                        throw new BookingHoldFailedException(ex.getMessage());
                    }
                }), externalHttpThreadPool)
                .handleAsync((result, ex) -> {
                    if (ex == null) {
                        return null;
                    }
                    throw new CompletionException(unwrap(ex));
                }, externalHttpThreadPool);
    }

    private CompletableFuture<Void> getSaveBookingFuture(AsyncBookingTaskEntity task) {
        return CompletableFuture
                .runAsync(() -> transactionTemplate.executeWithoutResult(status -> {
                    BookingEntity booking = findBookingEntity(task);
                    log.info("Finalizing booking persistence: bookingId={}, taskId={}", task.getBookingId(), task.getId());
                    if (booking.getPriceAmount() == null || booking.getHoldExpiresAt() == null) {
                        log.warn("Illegal booking state. Impossible to save booking with id={}", booking.getId());
                        throw new IllegalBookingStateException(
                                "Booking is not fully prepared for hold id=%s".formatted(booking.getId())
                        );
                    }
                    AsyncBookingTaskEntity savedTask = taskRepository.save(task);
                    log.info("Booking persistence finalized successfully: bookingId={}, taskId={}",
                            savedTask.getBookingId(),
                            savedTask.getId()
                    );
                }), externalHttpThreadPool)
                .handleAsync((result, ex) -> {
                    if (ex == null) {
                        return null;
                    }
                    throw new CompletionException(unwrap(ex));
                }, externalHttpThreadPool);
    }

    private void logCurrentProcessingStep(AsyncBookingTaskEntity task) {
        log.info("Task processing step={}, taskId={}, bookingId={}",
                task.getProcessingStep().getValue(),
                task.getId(),
                task.getBookingId()
        );
    }

    private long externalCallTimeoutMillis() {
        return properties.getExternalCallTimeout().toMillis();
    }

    private Throwable unwrap(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null
                && (current instanceof ExecutionException || current instanceof CompletionException)) {
            if (current.getCause() == current) {
                break;
            }
            current = current.getCause();
        }
        return current;
    }

    private BookingEntity findBookingEntity(AsyncBookingTaskEntity task) {
        UUID bookingId = task.getBookingId();
        BookingEntity booking = bookingTaskStateService.findBookingEntity(bookingId);
        if (booking == null) {
            log.warn("Booking not found for task processing: bookingId={}", bookingId);
            throw new BookingNotFoundException("Booking not found for task processing");
        }
        return booking;
    }
}
