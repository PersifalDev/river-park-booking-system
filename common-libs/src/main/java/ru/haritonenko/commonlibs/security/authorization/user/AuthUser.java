package ru.haritonenko.commonlibs.security.authorization.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;

@Builder
public record AuthUser(
        @NotNull(message = "User id can not be null")
        Long id,
        @NotBlank(message = "User login can not be blank")
        String login,
        @NotBlank(message = "User role can not be blank")
        String role
) {
}