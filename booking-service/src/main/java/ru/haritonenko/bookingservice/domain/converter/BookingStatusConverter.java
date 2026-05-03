package ru.haritonenko.bookingservice.domain.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.springframework.stereotype.Component;
import ru.haritonenko.bookingservice.domain.status.BookingStatus;
import ru.haritonenko.commonlibs.utils.EnumUtils;


@Converter
@Component
public class BookingStatusConverter implements AttributeConverter<BookingStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(BookingStatus typeRoomCategoryName) {
        return typeRoomCategoryName == null ? null : typeRoomCategoryName.getCode();
    }

    @Override
    public BookingStatus convertToEntityAttribute(Integer intCode) {
        return intCode == null ? null : EnumUtils.fromCode(BookingStatus.class, intCode);
    }
}
