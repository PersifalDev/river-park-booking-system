package ru.haritonenko.bookingservice.domain.tariff;

import java.time.LocalDate;

public record TariffSearchContext(
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Integer guests,
        Integer adultCount,
        Integer childrenCount,
        long nights
) {
}
