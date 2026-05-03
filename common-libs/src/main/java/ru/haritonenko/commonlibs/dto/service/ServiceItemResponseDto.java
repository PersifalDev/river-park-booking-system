package ru.haritonenko.commonlibs.dto.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ServiceItemResponseDto(
        Long id,
        String type,
        String title,
        String description,
        String photoUrl
) {
}
