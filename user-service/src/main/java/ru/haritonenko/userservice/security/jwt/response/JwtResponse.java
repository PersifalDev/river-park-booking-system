package ru.haritonenko.userservice.security.jwt.response;

import java.time.OffsetDateTime;

public record JwtResponse(
        String jwt,
        String accessToken,
        String refreshToken,
        OffsetDateTime accessTokenExpiresAt,
        OffsetDateTime refreshTokenExpiresAt
) {

    public static JwtResponse of(
            String accessToken,
            String refreshToken,
            OffsetDateTime accessTokenExpiresAt,
            OffsetDateTime refreshTokenExpiresAt
    ) {
        return new JwtResponse(accessToken, accessToken, refreshToken, accessTokenExpiresAt, refreshTokenExpiresAt);
    }
}
