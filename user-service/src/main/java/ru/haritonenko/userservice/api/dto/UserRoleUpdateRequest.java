package ru.haritonenko.userservice.api.dto;

import jakarta.validation.constraints.NotNull;
import ru.haritonenko.userservice.domain.UserRole;

public record UserRoleUpdateRequest(
        @NotNull(message = "User role can not be null")
        UserRole role
) {
}
