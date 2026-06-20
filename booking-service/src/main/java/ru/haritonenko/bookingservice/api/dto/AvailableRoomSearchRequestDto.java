package ru.haritonenko.bookingservice.api.dto;

import ru.haritonenko.bookingservice.domain.custom.validation.BookingDateRangeData;
import ru.haritonenko.bookingservice.domain.custom.validation.GuestCountData;
import ru.haritonenko.bookingservice.domain.custom.validation.annotation.ValidAvailableRoomSearchRequest;
import ru.haritonenko.bookingservice.domain.custom.validation.annotation.ValidBookingDateRange;
import ru.haritonenko.bookingservice.domain.custom.validation.annotation.ValidGuestCounts;
import ru.haritonenko.commonlibs.dto.category.type.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;

@ValidBookingDateRange(
        checkInRequired = true,
        checkOutRequired = true,
        pastAllowed = false
)
@ValidGuestCounts(
        guestsRequired = false
)
@ValidAvailableRoomSearchRequest
public record AvailableRoomSearchRequestDto(

        LocalDate checkInDate,

        LocalDate checkOutDate,

        Integer guests,

        Integer adultCount,

        Integer childrenCount,

        RoomType roomType,

        BigDecimal priceFrom,

        BigDecimal priceTo,

        BigDecimal minArea
) implements BookingDateRangeData, GuestCountData {
}
