package ru.haritonenko.telegrambot.config;

import jakarta.validation.constraints.Positive;

public record BotFlowBookingProperties(
        @Positive int maxGuests,
        @Positive int maxAdults,
        @Positive int maxChildren
) {
}
