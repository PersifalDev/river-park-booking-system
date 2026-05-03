package ru.haritonenko.userservice.domain;

public record User(
        Long id,
        String login,
        UserRole role
) {
}
