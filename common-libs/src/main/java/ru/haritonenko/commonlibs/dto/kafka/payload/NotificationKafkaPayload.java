package ru.haritonenko.commonlibs.dto.kafka.payload;

import ru.haritonenko.commonlibs.dto.kafka.event.type.NotificationEventType;
import ru.haritonenko.commonlibs.notification.NotificationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record NotificationKafkaPayload(
        UUID notificationId,
        Long userId,
        UUID bookingId,
        UUID paymentId,
        String title,
        String message,
        NotificationEventType notificationType,
        NotificationStatus status,
        OffsetDateTime createdAt
) {
}