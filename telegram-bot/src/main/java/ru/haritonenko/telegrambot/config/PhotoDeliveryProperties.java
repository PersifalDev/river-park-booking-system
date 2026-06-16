package ru.haritonenko.telegrambot.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "bot.photo")
public record PhotoDeliveryProperties(
        @Min(1) int byteCacheMaxEntries,
        @NotNull Duration connectTimeout,
        @NotNull Duration requestTimeout,
        @NotNull Duration telegramFileIdTtl
) {
}
