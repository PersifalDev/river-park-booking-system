package ru.haritonenko.bookingservice.observability;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;
import ru.haritonenko.bookingservice.domain.status.BookingStatus;
import ru.haritonenko.bookingservice.kafka.outbox.db.repository.BookingOutboxRepository;
import ru.haritonenko.bookingservice.kafka.outbox.status.OutboxStatus;
import ru.haritonenko.bookingservice.tasks.domain.async.db.repository.AsyncBookingTaskEntityRepository;
import ru.haritonenko.bookingservice.tasks.domain.async.status.AsyncBookingTaskStatus;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class BookingMetrics {

    private final Map<BookingStatus, Counter> bookingEvents;
    private final Counter polledTasks;
    private final Counter dispatchedOutboxEvents;
    private final AtomicInteger lastTaskPollBatchSize = new AtomicInteger();
    private final AtomicInteger lastOutboxBatchSize = new AtomicInteger();

    public BookingMetrics(
            MeterRegistry meterRegistry,
            BookingOutboxRepository outboxRepository,
            AsyncBookingTaskEntityRepository taskRepository
    ) {
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
            counter.increment();
        }
    }

    public void recordTaskPoll(int batchSize) {
        lastTaskPollBatchSize.set(batchSize);
        polledTasks.increment(batchSize);
    }

    public void recordOutboxPoll(int batchSize) {
        lastOutboxBatchSize.set(batchSize);
        dispatchedOutboxEvents.increment(batchSize);
    }

    private String tag(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
