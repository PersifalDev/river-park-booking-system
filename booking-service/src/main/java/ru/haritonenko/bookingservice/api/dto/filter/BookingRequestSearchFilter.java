package ru.haritonenko.bookingservice.api.dto.filter;

import lombok.Builder;
import ru.haritonenko.bookingservice.domain.custom.validation.BookingDateRangeData;
import ru.haritonenko.bookingservice.domain.custom.validation.GuestCountData;
import ru.haritonenko.bookingservice.domain.custom.validation.annotation.ValidBookingDateRange;
import ru.haritonenko.bookingservice.domain.custom.validation.annotation.ValidGuestCounts;
import ru.haritonenko.bookingservice.domain.status.BookingStatus;

import java.time.LocalDate;

@ValidBookingDateRange(
        checkInRequired = false,
        checkOutRequired = false,
        pastAllowed = true
)
@ValidGuestCounts
@Builder
public record BookingRequestSearchFilter(

        BookingStatus status,

        Boolean active,

        Integer adultCount,

        Integer childrenCount,

        LocalDate checkInDate,

        LocalDate checkOutDate
) implements BookingDateRangeData, GuestCountData {
}