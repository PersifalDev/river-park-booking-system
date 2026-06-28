package ru.haritonenko.telegrambot.dto.auth;

import java.time.OffsetDateTime;

public record BotJwtResponse(
        String jwt,
        String accessToken,
        String refreshToken,
        OffsetDateTime accessTokenExpiresAt,
        OffsetDateTime refreshTokenExpiresAt
) {
}
