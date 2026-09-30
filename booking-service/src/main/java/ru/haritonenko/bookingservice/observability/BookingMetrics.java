package ru.haritonenko.bookingservice.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ru.haritonenko.bookingservice.domain.status.BookingStatus;
import ru.haritonenko.bookingservice.kafka.outbox.db.repository.BookingOutboxRepository;
import ru.haritonenko.bookingservice.kafka.outbox.status.OutboxStatus;
import ru.haritonenko.bookingservice.tasks.domain.async.db.repository.AsyncBookingTaskEntityRepository;
import ru.haritonenko.bookingservice.tasks.domain.async.status.AsyncBookingTaskStatus;

import java.util.EnumMap;
import java.util.List;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class BookingMetrics {

    private final Map<BookingStatus, Counter> bookingEvents;
    private final Counter polledTasks;
    private final Counter dispatchedOutboxEvents;
    private final Timer createdToHold;
    private final AtomicInteger lastTaskPollBatchSize = new AtomicInteger();
    private final AtomicInteger lastOutboxBatchSize = new AtomicInteger();

    public BookingMetrics(
            MeterRegistry meterRegistry,
            BookingOutboxRepository outboxRepository,
            AsyncBookingTaskEntityRepository taskRepository
    ) {
        createdToHold = Timer.builder("booking_created_to_hold")
                .description("Time from booking creation to committed HOLD")
                .publishPercentileHistogram().minimumExpectedValue(Duration.ofMillis(1))
                .maximumExpectedValue(Duration.ofMinutes(2)).register(meterRegistry);
        bookingEvents = new EnumMap<>(BookingStatus.class);
        for (BookingStatus status : BookingStatus.values()) {
            bookingEvents.put(status, Counter.builder("booking_events_total")
                    .description("Booking lifecycle events")
                    .tag("status", tag(status.name()))
                    .register(meterRegistry));
        }
        for (OutboxStatus status : OutboxStatus.values()) {
            Gauge.builder("booking_outbox_backlog", outboxRepository, repository -> repository.countByStatus(status))
                    .description("Booking outbox records by status")
                    .tag("status", tag(status.name()))
                    .register(meterRegistry);
        }
        for (AsyncBookingTaskStatus status : AsyncBookingTaskStatus.values()) {
            Gauge.builder("async_booking_task_backlog", taskRepository, repository -> repository.countByStatus(status))
                    .description("Async booking tasks by status")
                    .tag("status", tag(status.name()))
                    .register(meterRegistry);
        }
        Gauge.builder("booking_task_oldest_pending_age_seconds", taskRepository, repository -> ageSeconds(
                        repository.findOldestCreatedAtByStatuses(List.of(AsyncBookingTaskStatus.NEW,
                                AsyncBookingTaskStatus.IN_PROGRESS, AsyncBookingTaskStatus.FAILED_RETRYABLE))))
                .description("Age of the oldest unfinished booking task").register(meterRegistry);
        Gauge.builder("booking_outbox_oldest_pending_age_seconds", outboxRepository, repository -> ageSeconds(
                        repository.findOldestCreatedAtByStatuses(List.of(OutboxStatus.NEW, OutboxStatus.PROCESSING))))
                .description("Age of the oldest undelivered booking event").register(meterRegistry);
        polledTasks = Counter.builder("booking_task_poller_tasks_total")
                .description("Async booking tasks picked by the poller")
                .register(meterRegistry);
        dispatchedOutboxEvents = Counter.builder("booking_outbox_dispatched_total")
                .description("Booking outbox events picked for dispatch")
                .register(meterRegistry);
        Gauge.builder("booking_task_poller_last_batch_size", lastTaskPollBatchSize, AtomicInteger::get)
                .description("Number of tasks picked during the latest poll")
                .register(meterRegistry);
        Gauge.builder("booking_outbox_last_batch_size", lastOutboxBatchSize, AtomicInteger::get)
                .description("Number of outbox events picked during the latest poll")
                .register(meterRegistry);
    }

    public void record(BookingStatus status) {
        Counter counter = bookingEvents.get(status);
        if (counter != null) {
            if (TransactionSynchronizationManager.isSynchronizationActive()
                    && TransactionSynchronizationManager.isActualTransactionActive()) {
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        counter.increment();
                    }
                });
            } else {
                counter.increment();
            }
        }
    }

    public void recordTaskPoll(int batchSize) {
        lastTaskPollBatchSize.set(batchSize);
        polledTasks.increment(batchSize);
    }

    public void recordHoldLatency(OffsetDateTime createdAt) {
        if (createdAt == null) {
            return;
        }
        Runnable record = () -> createdToHold.record(Duration.ofMillis(Math.max(0,
                Duration.between(createdAt, OffsetDateTime.now()).toMillis())));
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    record.run();
                }
            });
        } else {
            record.run();
        }
    }

    public void recordOutboxPoll(int batchSize) {
        lastOutboxBatchSize.set(batchSize);
        dispatchedOutboxEvents.increment(batchSize);
    }

    private String tag(String value) {
        return value.toLowerCase(Locale.ROOT);
    }

    private static double ageSeconds(OffsetDateTime createdAt) {
        return createdAt == null ? 0 : Math.max(0, Duration.between(createdAt, OffsetDateTime.now()).toMillis() / 1000.0);
    }
}
