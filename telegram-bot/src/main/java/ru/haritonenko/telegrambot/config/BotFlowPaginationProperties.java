package ru.haritonenko.telegrambot.config;

import jakarta.validation.constraints.Positive;

public record BotFlowPaginationProperties(
        @Positive int roomPageSize,
        @Positive int servicePageSize,
        @Positive int photoPageSize,
        @Positive int bookingsPageSize,
        @Positive int notificationsPageSize
) {
}
