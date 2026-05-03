package ru.haritonenko.commonlibs.dto.kafka.event;

import lombok.Builder;
import ru.haritonenko.commonlibs.dto.kafka.event.type.BookingEventType;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record BookingKafkaEvent<T>(
        UUID eventId,
        BookingEventType eventType,
        String source,
        String correlationId,
        OffsetDateTime createdAt,
        T payload
) {
}