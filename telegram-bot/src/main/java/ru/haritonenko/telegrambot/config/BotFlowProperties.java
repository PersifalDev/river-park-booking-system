package ru.haritonenko.telegrambot.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "bot.flow")
public record BotFlowProperties(
        @Valid @NotNull BotFlowBookingProperties booking,
        @Valid @NotNull BotFlowFilterProperties filter,
        @Valid @NotNull BotFlowPaginationProperties pagination
) {
}
