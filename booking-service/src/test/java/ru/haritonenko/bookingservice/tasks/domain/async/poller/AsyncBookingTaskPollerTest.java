package ru.haritonenko.bookingservice.tasks.domain.async.poller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import ru.haritonenko.bookingservice.domain.db.entity.BookingEntity;
import ru.haritonenko.bookingservice.domain.service.BookingService;
import ru.haritonenko.bookingservice.observability.BookingMetrics;
import ru.haritonenko.bookingservice.tasks.domain.async.db.entity.AsyncBookingTaskEntity;
import ru.haritonenko.bookingservice.tasks.domain.async.db.repository.AsyncBookingTaskEntityRepository;
import ru.haritonenko.bookingservice.tasks.domain.async.dispatcher.AsyncBookingTaskDispatcher;
import ru.haritonenko.bookingservice.tasks.domain.async.poller.config.AsyncBookingTaskPollerProperties;
import ru.haritonenko.bookingservice.tasks.domain.async.status.AsyncBookingTaskStatus;
import ru.haritonenko.bookingservice.tasks.domain.async.status.ProcessingStep;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AsyncBookingTaskPollerTest {

    private final AsyncBookingTaskEntityRepository taskRepository = mock(AsyncBookingTaskEntityRepository.class);
    private final BookingService bookingService = mock(BookingService.class);
    private final TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
    private final AsyncBookingTaskDispatcher taskDispatcher = mock(AsyncBookingTaskDispatcher.class);
    private final AsyncBookingTaskPollerProperties properties = new AsyncBookingTaskPollerProperties();
    private final BookingMetrics bookingMetrics = mock(BookingMetrics.class);

    private final AsyncBookingTaskPoller poller =
            new AsyncBookingTaskPoller(
                    taskRepository,
                    bookingService,
                    transactionTemplate,
                    taskDispatcher,
                    properties,
                    bookingMetrics
            );

    @BeforeEach
    void setUp() {
        properties.setBatchSize(5);
        properties.setRetryDelay(Duration.ofSeconds(30));
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
    }

    @Test
    void shouldDispatchPickedTasks() {
        AsyncBookingTaskEntity task = task();
        when(taskRepository.pickBatchForProcessing(anyInt(), anyInt(), anyInt(), anyInt(), any()))
                .thenReturn(List.of(task));

        poller.poll();

        assertEquals(AsyncBookingTaskStatus.IN_PROGRESS, task.getStatus());
        verify(taskRepository).saveAll(List.of(task));
        verify(taskDispatcher).dispatchTask(task);
        verify(bookingMetrics).recordTaskPoll(1);
    }

    @Test
    void shouldNotDispatchWhenNoTasksPicked() {
        when(taskRepository.pickBatchForProcessing(anyInt(), anyInt(), anyInt(), anyInt(), any()))
                .thenReturn(List.of());

        poller.poll();

        verify(taskDispatcher, never()).dispatchTask(any());
        verify(bookingMetrics).recordTaskPoll(0);
    }

    @Test
    void shouldExpireHoldAndCreatedBookingsAndReleaseCompletedInventory() {
        BookingEntity hold = BookingEntity.builder().id(UUID.randomUUID()).build();
        BookingEntity created = BookingEntity.builder().id(UUID.randomUUID()).build();
        BookingEntity confirmed = BookingEntity.builder().id(UUID.randomUUID()).build();
        when(bookingService.findExpiredHoldBookings()).thenReturn(List.of(hold));
        when(bookingService.findExpiredCreatedBookings()).thenReturn(List.of(created));
        when(bookingService.findConfirmedBookingsForInventoryRelease(any(LocalDate.class))).thenReturn(List.of(confirmed));

        poller.expireHoldBookings();

        verify(bookingService).expireBooking(hold.getId());
        verify(bookingService).expireCreatedBooking(created.getId());
        verify(bookingService).releaseInventoryAfterCheckOut(confirmed.getId());
    }

    @Test
    void shouldCleanupInactiveAndCompletedBookings() {
        OffsetDateTime threshold = OffsetDateTime.now().minusDays(1);
        LocalDate completedThreshold = LocalDate.now().minusMonths(1);
        when(bookingService.getCleanupThreshold()).thenReturn(threshold);
        when(bookingService.getCompletedCleanupThresholdDate()).thenReturn(completedThreshold);

        poller.cleanupInactiveBookings();

        verify(bookingService).deleteInactiveBookingsCreatedBefore(threshold);
        verify(bookingService).deleteCompletedBookingsCheckedOutBefore(completedThreshold);
    }

    private AsyncBookingTaskEntity task() {
        return AsyncBookingTaskEntity.builder()
                .id(1L)
                .bookingId(UUID.randomUUID())
                .status(AsyncBookingTaskStatus.NEW)
                .processingStep(ProcessingStep.VALIDATE_REQUEST)
                .attempts(0)
                .build();
    }
}
