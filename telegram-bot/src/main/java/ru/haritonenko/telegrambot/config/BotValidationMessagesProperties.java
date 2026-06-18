package ru.haritonenko.telegrambot.config;

import jakarta.validation.constraints.NotBlank;

public record BotValidationMessagesProperties(
        @NotBlank String bookingAlreadyInactive,
        @NotBlank String dateError,
        @NotBlank String filterError,
        @NotBlank String defaultError,
        @NotBlank String promoIgnored,
        @NotBlank String priceRange,
        @NotBlank String areaRange
) {
}
