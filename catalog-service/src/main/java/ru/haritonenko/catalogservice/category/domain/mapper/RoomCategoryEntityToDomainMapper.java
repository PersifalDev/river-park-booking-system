package ru.haritonenko.catalogservice.category.domain.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.haritonenko.catalogservice.category.domain.RoomCategory;
import ru.haritonenko.catalogservice.category.domain.db.entity.RoomCategoryEntity;

@Mapper(componentModel = "spring")
public interface RoomCategoryEntityToDomainMapper {

    @Mapping(target = "mainPhotoPath", ignore = true)
    RoomCategory toDomain(RoomCategoryEntity entity);
}