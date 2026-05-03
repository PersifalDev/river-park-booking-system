package ru.haritonenko.commonlibs.dto.users;

public record UserResponseDto(
        Long id,
        String login,
        String role,
        Long telegramId,
        String fullName,
        String pageNumber,
        String username
) {
}
