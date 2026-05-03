package ru.haritonenko.bookingservice.domain.mapper;

import org.mapstruct.Mapper;
import ru.haritonenko.bookingservice.domain.Booking;
import ru.haritonenko.bookingservice.domain.db.entity.BookingEntity;

@Mapper(componentModel = "spring")
public interface BookingToDomainMapper {

    Booking toDomain(BookingEntity bookingEntity);
}
