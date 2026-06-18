package ru.haritonenko.telegrambot.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "bot.messages")
public record BotMessagesProperties(
        @Valid @NotNull BotValidationMessagesProperties validation,
        @Valid @NotNull BotBookingMessagesProperties booking,
        @Valid @NotNull BotInputMessagesProperties input,
        @Valid @NotNull BotNotificationMessagesProperties notification,
        @Valid @NotNull BotServiceUnavailableMessagesProperties serviceUnavailable,
        @Valid @NotNull BotKeyboardMessagesProperties keyboard
) {
}
