package ru.haritonenko.telegrambot.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "telegram.bot")
public record BotProperties(
        @NotBlank String token,
        @NotBlank String username,
        String adminContact
) {
}
