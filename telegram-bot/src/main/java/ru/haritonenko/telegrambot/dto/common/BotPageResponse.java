package ru.haritonenko.telegrambot.dto.common;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BotPageResponse<T>(
        List<T> content,
        int totalPages,
        long totalElements,
        int size,
        int number,
        boolean first,
        boolean last,
        boolean empty
) {
}
