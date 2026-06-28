package ru.haritonenko.telegrambot.dto.auth;

public record BotUserRegistrationRequest(
        String login,
        String key,
        Boolean personalDataConsentAccepted,
        Boolean privacyPolicyAccepted
) {
}
