package ru.haritonenko.telegrambot.config;

import jakarta.validation.constraints.NotBlank;

public record BotServiceUnavailableMessagesProperties(
        @NotBlank String booking,
        @NotBlank String catalog,
        @NotBlank String payment,
        @NotBlank String notification,
        @NotBlank String user,
        @NotBlank String defaultMessage
) {
}
