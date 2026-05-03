package ru.haritonenko.commonlibs.dto.kafka.event;

import lombok.Builder;
import ru.haritonenko.commonlibs.dto.kafka.event.type.CatalogEventType;

import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record CatalogKafkaEvent<T>(
        UUID eventId,
        CatalogEventType eventType,
        String source,
        String correlationId,
        OffsetDateTime createdAt,
        T payload
) {

}
