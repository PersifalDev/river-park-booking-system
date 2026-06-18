package ru.haritonenko.telegrambot.config;

import jakarta.validation.constraints.NotBlank;

public record BotNotificationMessagesProperties(
        @NotBlank String automaticInfo,
        @NotBlank String noneUnread,
        @NotBlank String markedRead,
        @NotBlank String allMarkedRead
) {
}
