package ru.haritonenko.telegrambot.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "clients.user")
public record UserClientProperties(
        @NotBlank String baseUrl
) {
}
