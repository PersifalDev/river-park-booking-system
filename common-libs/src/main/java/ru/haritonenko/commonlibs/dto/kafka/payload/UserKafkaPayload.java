package ru.haritonenko.commonlibs.dto.kafka.payload;

import lombok.Builder;

@Builder
public record UserKafkaPayload(
        Long userId,
        Long telegramId,
        String login,
        String firstName,
        String lastName,
        String phone,
        String email,
        String role,
        Boolean registered,
        Boolean active
) {
}