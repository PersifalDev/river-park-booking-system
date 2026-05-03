package ru.haritonenko.catalogservice.services.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder(toBuilder = true)
public record ServiceItemResponseDto(
        Long id,
        String type,
        String title,
        String description,
        String photoUrl
) {
}
