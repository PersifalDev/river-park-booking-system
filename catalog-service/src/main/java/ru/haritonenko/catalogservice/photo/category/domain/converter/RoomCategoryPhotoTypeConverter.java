package ru.haritonenko.catalogservice.photo.category.domain.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;
import ru.haritonenko.catalogservice.photo.category.domain.type.RoomCategoryPhotoType;
import ru.haritonenko.commonlibs.utils.EnumUtils;

@Converter
@Component
public class RoomCategoryPhotoTypeConverter implements AttributeConverter<RoomCategoryPhotoType, Integer> {
    @Override
    public Integer convertToDatabaseColumn(RoomCategoryPhotoType roomCategoryPhotoType) {
        return roomCategoryPhotoType == null ? null : roomCategoryPhotoType.getCode();
    }

    @Override
    public RoomCategoryPhotoType convertToEntityAttribute(Integer intCode) {
        return intCode == null ? null : EnumUtils.fromCode(RoomCategoryPhotoType.class, intCode);
    }
}
