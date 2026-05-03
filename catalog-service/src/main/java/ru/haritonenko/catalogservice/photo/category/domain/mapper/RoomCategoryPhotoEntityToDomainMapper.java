package ru.haritonenko.catalogservice.photo.category.domain.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.haritonenko.catalogservice.photo.category.domain.RoomCategoryPhoto;
import ru.haritonenko.catalogservice.photo.category.domain.db.entity.RoomCategoryPhotoEntity;

@Mapper(componentModel = "spring")
public interface RoomCategoryPhotoEntityToDomainMapper {

    @Mapping(target = "roomCategoryId", source = "roomCategory.id")
    RoomCategoryPhoto toDomain(RoomCategoryPhotoEntity photoEntity);
}