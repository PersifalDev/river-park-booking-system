package ru.haritonenko.catalogservice.photo.category.domain;

import lombok.Builder;
import ru.haritonenko.catalogservice.photo.category.domain.type.RoomCategoryPhotoType;

@Builder(toBuilder=true)
public record RoomCategoryPhoto(
        Long id,
        Long roomCategoryId,
        String path,
        RoomCategoryPhotoType photoType
) {
}
