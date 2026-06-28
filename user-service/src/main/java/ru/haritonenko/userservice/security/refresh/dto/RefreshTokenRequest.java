package ru.haritonenko.userservice.security.refresh.dto;

import jakarta.validation.constraints.NotBlank;

public record RefreshTokenRequest(
        @NotBlank(message = "Refresh token can not be blank")
        String refreshToken
) {
}
