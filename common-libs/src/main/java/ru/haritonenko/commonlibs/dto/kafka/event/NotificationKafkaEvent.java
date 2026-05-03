package ru.haritonenko.commonlibs.dto.kafka.event;

import ru.haritonenko.commonlibs.dto.kafka.event.type.NotificationEventType;
import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationKafkaEvent<T>(
        UUID eventId,
        NotificationEventType eventType,
        String source,
        String correlationId,
        OffsetDateTime createdAt,
        T payload
) {
}