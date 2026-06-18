package ru.haritonenko.telegrambot.config;

import jakarta.validation.constraints.NotBlank;

public record BotBookingMessagesProperties(
        @NotBlank String maxGuests,
        @NotBlank String selectedPeriod,
        @NotBlank String cancelling,
        @NotBlank String inactiveEmpty,
        @NotBlank String inactiveTitle,
        @NotBlank String earlyEmpty,
        @NotBlank String earlyTitle,
        @NotBlank String historyEmpty,
        @NotBlank String historyTitle,
        @NotBlank String roomDefaultTitle,
        @NotBlank String nextPromoCode
) {
}
