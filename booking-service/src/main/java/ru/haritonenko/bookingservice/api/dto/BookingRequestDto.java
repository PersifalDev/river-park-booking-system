package ru.haritonenko.bookingservice.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import ru.haritonenko.bookingservice.domain.custom.validation.BookingDateRangeData;
import ru.haritonenko.bookingservice.domain.custom.validation.GuestCountData;
import ru.haritonenko.bookingservice.domain.custom.validation.annotation.ValidBookingDateRange;
import ru.haritonenko.bookingservice.domain.custom.validation.annotation.ValidGuestCounts;

import java.time.LocalDate;

@ValidBookingDateRange(
        checkInRequired = true,
        checkOutRequired = true,
        pastAllowed = false
)
@ValidGuestCounts(
        guestsRequired = true,
        adultCountRequired = true,
        childrenCountRequired = true,
        validateComposition = true
)
@Builder
public record BookingRequestDto(

        @NotNull(message = "Category id can not be null")
        @Positive(message = "Category id must be positive")
        Long categoryId,

        LocalDate checkInDate,

        LocalDate checkOutDate,

        Integer guests,

        Integer adultCount,

        Integer childrenCount,

        @Size(max = 50, message = "Promo code is too long")
        String promoCode
) implements BookingDateRangeData, GuestCountData {
}