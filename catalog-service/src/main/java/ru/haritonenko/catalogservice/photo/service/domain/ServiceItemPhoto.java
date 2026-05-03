package ru.haritonenko.catalogservice.photo.service.domain;

import lombok.Builder;

@Builder(toBuilder = true)
public record ServiceItemPhoto(
        Long id,
        Long serviceItemId,
        String path
) {
}
