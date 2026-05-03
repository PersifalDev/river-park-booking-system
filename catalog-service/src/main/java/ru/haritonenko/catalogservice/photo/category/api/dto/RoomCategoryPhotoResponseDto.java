package ru.haritonenko.catalogservice.photo.category.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder(toBuilder = true)
public record RoomCategoryPhotoResponseDto(
        Long id,
        String url,
        String photoType
) {
}