package ru.haritonenko.bookingservice.api.dto.filter;

import lombok.Builder;
import ru.haritonenko.bookingservice.domain.status.BookingStatus;

import java.time.LocalDate;

@Builder(toBuilder = true)
public record BookingRequestSearchFilter(
        BookingStatus status,
        Boolean active,
        Integer adultCount,
        Integer childrenCount,
        LocalDate checkInDate,
        LocalDate checkOutDate
) {
}
