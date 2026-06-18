package ru.haritonenko.telegrambot.config;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record BotFlowFilterProperties(
        @Positive int maxGuests,
        @NotNull @DecimalMin("0.0") BigDecimal minPrice,
        @NotNull @DecimalMin("0.0") BigDecimal maxPrice,
        @NotNull @DecimalMin("0.0") BigDecimal minArea,
        @NotNull @DecimalMin("0.0") BigDecimal maxArea
) {
}
