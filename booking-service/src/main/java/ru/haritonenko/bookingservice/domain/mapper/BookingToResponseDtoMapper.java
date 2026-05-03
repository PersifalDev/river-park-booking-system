package ru.haritonenko.bookingservice.domain.mapper;

import org.mapstruct.Mapper;
import ru.haritonenko.bookingservice.api.dto.BookingResponseDto;
import ru.haritonenko.bookingservice.domain.Booking;

@Mapper(componentModel = "spring")
public interface BookingToResponseDtoMapper {

    BookingResponseDto toDto(Booking booking);
}