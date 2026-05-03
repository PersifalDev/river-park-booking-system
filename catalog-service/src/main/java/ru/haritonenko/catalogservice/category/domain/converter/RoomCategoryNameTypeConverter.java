package ru.haritonenko.catalogservice.category.domain.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;
import ru.haritonenko.catalogservice.category.domain.type.RoomType;
import ru.haritonenko.commonlibs.utils.EnumUtils;

@Converter
@Component
public class RoomCategoryNameTypeConverter implements AttributeConverter<RoomType, Integer> {
    @Override
    public Integer convertToDatabaseColumn(RoomType typeRoomCategoryName) {
        return typeRoomCategoryName == null ? null : typeRoomCategoryName.getCode();
    }

    @Override
    public RoomType convertToEntityAttribute(Integer intCode) {
        return intCode == null ? null : EnumUtils.fromCode(RoomType.class, intCode);
    }
}
