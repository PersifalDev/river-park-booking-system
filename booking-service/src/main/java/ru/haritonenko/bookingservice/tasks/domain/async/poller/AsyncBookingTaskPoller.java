package ru.haritonenko.bookingservice.tasks.domain.async.poller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import ru.haritonenko.bookingservice.domain.service.BookingService;
import ru.haritonenko.bookingservice.tasks.domain.async.db.entity.AsyncBookingTaskEntity;
import ru.haritonenko.bookingservice.tasks.domain.async.db.repository.AsyncBookingTaskEntityRepository;
import ru.haritonenko.bookingservice.tasks.domain.async.dispatcher.AsyncBookingTaskDispatcher;
import ru.haritonenko.bookingservice.tasks.domain.async.poller.config.AsyncBookingTaskPollerProperties;
import ru.haritonenko.bookingservice.tasks.domain.async.status.AsyncBookingTaskStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@Slf4j
@RequiredArgsConstructor
@Component
public class AsyncBookingTaskPoller {

    private final AsyncBookingTaskEntityRepository taskRepository;
    private final BookingService bookingService;
    private final TransactionTemplate transactionTemplate;
    private final AsyncBookingTaskDispatcher taskDispatcher;
    private final AsyncBookingTaskPollerProperties properties;

    @Scheduled(fixedDelayString = "${app.task-execution.poller.poll-interval-ms}")
    public void poll() {
        log.debug("Starting async booking task polling");
        List<AsyncBookingTaskEntity> tasksBatch = pickTasksForProcessing();

        if (tasksBatch.isEmpty()) {
            log.debug("No tasks found for polling");
            return;
        }

        var taskIds = tasksBatch.stream()
                .filter(Objects::nonNull)
                .map(AsyncBookingTaskEntity::getId)
                .toList();
        log.info("Picked tasks for polling: count={}, taskIds={}", taskIds.size(), taskIds);

        for (AsyncBookingTaskEntity task : tasksBatch) {
            taskDispatcher.dispatchTask(task);
        }
    }

    @Scheduled(fixedDelayString = "${app.booking.hold-expiration-delay-ms}")
    public void expireHoldBookings() {
        log.debug("Starting expired booking scan");
        bookingService.findExpiredHoldBookings().forEach(booking -> {
            log.info("Expiring hold booking: bookingId={}", booking.getId());
            bookingService.expireBooking(booking.getId());
        });
        bookingService.findExpiredCreatedBookings().forEach(booking -> {
            log.info("Expiring created booking: bookingId={}", booking.getId());
            bookingService.expireCreatedBooking(booking.getId());
        });
    }

    @Scheduled(fixedDelayString = "${app.booking.cleanup.delay-ms}")
    public void cleanupInactiveBookings() {
        log.debug("Starting inactive bookings cleanup");
        bookingService.deleteInactiveBookingsCreatedBefore(bookingService.getCleanupThreshold());
    }

    private List<AsyncBookingTaskEntity> pickTasksForProcessing() {
        log.debug("Picking tasks for processing");
        return transactionTemplate.execute(status -> {
            List<AsyncBookingTaskEntity> tasks = taskRepository.pickBatchForProcessing(
                    AsyncBookingTaskStatus.NEW.getCode(),
                    AsyncBookingTaskStatus.FAILED_RETRYABLE.getCode(),
                    AsyncBookingTaskStatus.IN_PROGRESS.getCode(),
                    properties.getBatchSize(),
                    OffsetDateTime.now()
            );
            OffsetDateTime nextProcessTime = OffsetDateTime.now().plus(properties.getRetryDelay());
            for (AsyncBookingTaskEntity task : tasks) {
                task.setStatus(AsyncBookingTaskStatus.IN_PROGRESS);
                task.setNextAttemptAt(nextProcessTime);
                log.debug("Task prepared for processing: taskId={}, bookingId={}, nextAttemptAt={}",
                        task.getId(),
                        task.getBookingId(),
                        task.getNextAttemptAt()
                );
            }
            taskRepository.saveAll(tasks);
            return tasks;
        });
    }
}
