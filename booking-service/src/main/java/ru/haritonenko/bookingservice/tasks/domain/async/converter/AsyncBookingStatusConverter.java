package ru.haritonenko.bookingservice.tasks.domain.async.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import ru.haritonenko.bookingservice.tasks.domain.async.status.AsyncBookingTaskStatus;
import ru.haritonenko.commonlibs.utils.EnumUtils;

@Converter
public class AsyncBookingStatusConverter implements AttributeConverter<AsyncBookingTaskStatus, Integer> {

    @Override
    public Integer convertToDatabaseColumn(AsyncBookingTaskStatus statusEnum) {
        return statusEnum == null
                ? null
                : statusEnum.getCode();
    }

    @Override
    public AsyncBookingTaskStatus convertToEntityAttribute(Integer intCode) {
        return intCode == null
                ? null
                : EnumUtils.fromCode(AsyncBookingTaskStatus.class, intCode);
    }
}
