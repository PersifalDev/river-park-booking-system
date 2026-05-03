package ru.haritonenko.commonlibs.dto.kafka.event;

import lombok.Builder;
import ru.haritonenko.commonlibs.dto.kafka.event.type.PaymentEventType;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record PaymentKafkaEvent<T>(
        UUID eventId,
        PaymentEventType eventType,
        String source,
        String correlationId,
        OffsetDateTime createdAt,
        T payload
) {
}
