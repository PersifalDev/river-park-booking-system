package ru.haritonenko.telegrambot.dto.auth;

public record BotUserRegistrationRequest(
        String login,
        String key
) {
}
