package ru.haritonenko.bookingservice.tasks.domain.async.processor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import ru.haritonenko.bookingservice.domain.db.entity.BookingEntity;
import ru.haritonenko.bookingservice.domain.service.BookingInventoryService;
import ru.haritonenko.bookingservice.domain.service.price.BookingPricingService;
import ru.haritonenko.bookingservice.tasks.domain.async.db.entity.AsyncBookingTaskEntity;
import ru.haritonenko.bookingservice.tasks.domain.async.db.repository.AsyncBookingTaskEntityRepository;
import ru.haritonenko.bookingservice.tasks.domain.async.dispatcher.config.AsyncBookingTaskDispatcherProperties;
import ru.haritonenko.bookingservice.tasks.domain.async.service.BookingTaskStateService;
import ru.haritonenko.bookingservice.tasks.domain.async.status.AsyncBookingTaskStatus;
import ru.haritonenko.bookingservice.tasks.domain.async.status.ProcessingStep;
import ru.haritonenko.bookingservice.tasks.domain.async.status.TaskExecutionStatus;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AsyncBookingTaskProcessorTest {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final BookingTaskStateService bookingTaskStateService = mock(BookingTaskStateService.class);
    private final BookingInventoryService bookingInventoryService = mock(BookingInventoryService.class);
    private final BookingPricingService bookingPricingService = mock(BookingPricingService.class);
    private final AsyncBookingTaskEntityRepository taskRepository = mock(AsyncBookingTaskEntityRepository.class);
    private final TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
    private final AsyncBookingTaskDispatcherProperties properties = new AsyncBookingTaskDispatcherProperties();

    private AsyncBookingTaskProcessor processor;

    @BeforeEach
    void setUp() {
        properties.setHoldTtl(Duration.ofMinutes(15));
        properties.setExternalCallTimeout(Duration.ofSeconds(5));
        processor = new AsyncBookingTaskProcessor(
                executorService,
                bookingTaskStateService,
                bookingInventoryService,
                bookingPricingService,
                taskRepository,
                transactionTemplate,
                properties
        );
        doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        }).when(transactionTemplate).execute(any());
        doAnswer(invocation -> {
            Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        when(taskRepository.save(any(AsyncBookingTaskEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doAnswer(invocation -> {
            BookingEntity booking = bookingTaskStateService.findBookingEntity(invocation.getArgument(0));
            booking.setPriceAmount(invocation.getArgument(1));
            return null;
        }).when(bookingTaskStateService).updateBookingPrice(any(), any());
        doAnswer(invocation -> {
            BookingEntity booking = bookingTaskStateService.findBookingEntity(invocation.getArgument(0));
            booking.setPriceAmount(invocation.getArgument(1));
            booking.setHoldExpiresAt(invocation.getArgument(2));
            return null;
        }).when(bookingTaskStateService).setBookingHold(any(), any(), any());
    }

    @AfterEach
    void tearDown() {
        executorService.shutdownNow();
    }

    @Test
    void shouldReturnNonRetryableWhenBookingNotFound() {
        AsyncBookingTaskEntity task = task(ProcessingStep.VALIDATE_REQUEST);
        when(bookingTaskStateService.existsBookingById(task.getBookingId())).thenReturn(false);

        TaskExecutionStatus actual = processor.processTask(task);

        assertEquals(TaskExecutionStatus.NON_RETRYABLE_ERROR, actual);
        verify(taskRepository, never()).save(any());
    }

    @Test
    void shouldMoveFromValidateToSuccessfulHoldFlow() {
        AsyncBookingTaskEntity task = task(ProcessingStep.VALIDATE_REQUEST);
        BookingEntity booking = booking(task.getBookingId());
        when(bookingTaskStateService.existsBookingById(task.getBookingId())).thenReturn(true);
        when(bookingTaskStateService.findBookingEntity(task.getBookingId())).thenReturn(booking);
        when(bookingInventoryService.isAvailable(booking)).thenReturn(true);
        when(bookingPricingService.calculatePrice(booking)).thenReturn(BigDecimal.valueOf(10000));
        when(bookingInventoryService.getTotalUnitsFromRoomCategory(1L)).thenReturn(10);

        TaskExecutionStatus actual = processor.processTask(task);

        assertEquals(TaskExecutionStatus.SUCCESS, actual);
        assertEquals(ProcessingStep.SAVE_BOOKING, task.getProcessingStep());
        verify(bookingTaskStateService).updateBookingPrice(task.getBookingId(), BigDecimal.valueOf(10000));
        verify(bookingInventoryService).holdInventory(booking, 10);
        verify(bookingTaskStateService).setBookingHold(any(), any(), any());
    }

    @Test
    void shouldReturnNonRetryableWhenAvailabilityCheckFails() {
        AsyncBookingTaskEntity task = task(ProcessingStep.CHECK_AVAILABILITY);
        BookingEntity booking = booking(task.getBookingId());
        when(bookingTaskStateService.existsBookingById(task.getBookingId())).thenReturn(true);
        when(bookingTaskStateService.findBookingEntity(task.getBookingId())).thenReturn(booking);
        when(bookingInventoryService.isAvailable(booking)).thenReturn(false);

        TaskExecutionStatus actual = processor.processTask(task);

        assertEquals(TaskExecutionStatus.NON_RETRYABLE_ERROR, actual);
        verify(bookingTaskStateService).markBookingFailed(task.getBookingId(), "No available rooms for requested period");
    }

    @Test
    void shouldContinueFromCreateHoldStep() {
        AsyncBookingTaskEntity task = task(ProcessingStep.CREATE_HOLD);
        BookingEntity booking = booking(task.getBookingId());
        booking.setPriceAmount(BigDecimal.valueOf(7000));
        when(bookingTaskStateService.existsBookingById(task.getBookingId())).thenReturn(true);
        when(bookingTaskStateService.findBookingEntity(task.getBookingId())).thenReturn(booking);
        when(bookingInventoryService.getTotalUnitsFromRoomCategory(1L)).thenReturn(5);

        TaskExecutionStatus actual = processor.processTask(task);

        assertEquals(TaskExecutionStatus.SUCCESS, actual);
        verify(bookingInventoryService).holdInventory(booking, 5);
        verify(bookingTaskStateService).setBookingHold(any(), any(), any());
    }

    private AsyncBookingTaskEntity task(ProcessingStep step) {
        return AsyncBookingTaskEntity.builder()
                .id(1L)
                .bookingId(UUID.randomUUID())
                .status(AsyncBookingTaskStatus.IN_PROGRESS)
                .processingStep(step)
                .attempts(1)
                .build();
    }

    private BookingEntity booking(UUID bookingId) {
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        return BookingEntity.builder()
                .id(bookingId)
                .userId(10L)
                .roomCategoryId(1L)
                .bookingCode("BK-TEST")
                .guests(2)
                .adultCount(2)
                .childrenCount(0)
                .checkInDate(checkInDate)
                .checkOutDate(checkInDate.plusDays(1))
                .priceAmount(BigDecimal.ONE)
                .hasPromo(false)
                .build();
    }
}
