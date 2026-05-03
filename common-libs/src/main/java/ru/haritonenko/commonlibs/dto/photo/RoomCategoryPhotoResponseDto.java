package ru.haritonenko.commonlibs.dto.photo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RoomCategoryPhotoResponseDto(
        Long id,
        String url,
        String photoType
) {
}