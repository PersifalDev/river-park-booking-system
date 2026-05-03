package ru.haritonenko.bookingservice.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import ru.haritonenko.bookingservice.domain.custom.validation.annotation.NotPastDate;
import ru.haritonenko.bookingservice.domain.custom.validation.annotation.ValidBookingRequest;

import java.time.LocalDate;

@ValidBookingRequest
public record BookingRequestDto(

        @NotNull(message = "Category id can not be null")
        Long categoryId,

        @NotNull(message = "Check in date can not be null")
        @NotPastDate(message = "Check in date can not be in the past")
        LocalDate checkInDate,

        @NotNull(message = "Check out date can not be null")
        @NotPastDate(message = "Check out date can not be in the past")
        LocalDate checkOutDate,

        @NotNull(message = "Guests count can not be null")
        @Min(value = 1, message = "Guests count must be greater than or equal to 1")
        Integer guests,

        @NotNull(message = "Adult count can not be null")
        @Min(value = 1, message = "Adult count must be greater than or equal to 1")
        Integer adultCount,

        @NotNull(message = "Children count can not be null")
        @Min(value = 0, message = "Children count must be greater than or equal to 0")
        Integer childrenCount,

        String promoCode
) {
}