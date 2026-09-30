package ru.haritonenko.bookingservice.observability;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ru.haritonenko.bookingservice.domain.status.BookingStatus;
import ru.haritonenko.bookingservice.kafka.outbox.db.repository.BookingOutboxRepository;
import ru.haritonenko.bookingservice.tasks.domain.async.db.repository.AsyncBookingTaskEntityRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class BookingMetricsTest {
    private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
    private final BookingMetrics metrics = new BookingMetrics(registry,
            mock(BookingOutboxRepository.class), mock(AsyncBookingTaskEntityRepository.class));

    @AfterEach
    void cleanup() {
        TransactionSynchronizationManager.clear();
        registry.close();
    }

    @Test
    void shouldCountCommittedTransitionOnlyAfterCommit() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        metrics.record(BookingStatus.HOLD);
        assertEquals(0, holdCount());
        TransactionSynchronizationManager.getSynchronizations().forEach(TransactionSynchronization::afterCommit);
        assertEquals(1, holdCount());
    }

    @Test
    void shouldNotCountRolledBackTransition() {
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        metrics.record(BookingStatus.HOLD);
        TransactionSynchronizationManager.getSynchronizations().forEach(
                sync -> sync.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));
        assertEquals(0, holdCount());
    }

    private double holdCount() {
        return registry.get("booking_events_total").tag("status", "hold").counter().count();
    }
}
