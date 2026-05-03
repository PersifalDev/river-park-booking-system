package ru.haritonenko.telegrambot.dto.notification;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.OffsetDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BotNotificationResponseDto(
        UUID id,
        Long userId,
        UUID bookingId,
        UUID paymentId,
        String title,
        String message,
        String type,
        String status,
        boolean read,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
