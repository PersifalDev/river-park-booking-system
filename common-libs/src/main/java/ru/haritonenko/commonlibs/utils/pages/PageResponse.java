package ru.haritonenko.commonlibs.utils.pages;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PageResponse<T>(
        List<T> content,
        int totalPages,
        long totalElements,
        int size,
        int number
) {
}
