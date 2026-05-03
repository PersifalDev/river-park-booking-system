package ru.haritonenko.commonlibs.dto.kafka.event;

import lombok.Builder;
import ru.haritonenko.commonlibs.dto.kafka.event.type.UserEventType;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record UserKafkaEvent<T>(
        UUID eventId,
        UserEventType eventType,
        String source,
        String correlationId,
        OffsetDateTime createdAt,
        T payload
) {
}
