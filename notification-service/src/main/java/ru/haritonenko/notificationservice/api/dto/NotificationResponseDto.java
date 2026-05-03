package ru.haritonenko.notificationservice.api.dto;

import ru.haritonenko.commonlibs.dto.kafka.event.type.NotificationEventType;
import ru.haritonenko.commonlibs.notification.NotificationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationResponseDto(
        UUID id,
        Long userId,
        UUID bookingId,
        UUID paymentId,
        String title,
        String message,
        NotificationEventType type,
        NotificationStatus status,
        boolean read,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
