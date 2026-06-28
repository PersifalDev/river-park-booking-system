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

@Component
public class BookingMetrics {

    private final Map<BookingStatus, Counter> bookingEvents;

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
    }

    public void record(BookingStatus status) {
        Counter counter = bookingEvents.get(status);
        if (counter != null) {
            counter.increment();
        }
    }

    private String tag(String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
