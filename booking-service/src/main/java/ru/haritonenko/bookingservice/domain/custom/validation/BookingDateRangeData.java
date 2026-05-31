package ru.haritonenko.bookingservice.domain.custom.validation;

import java.time.LocalDate;

public interface BookingDateRangeData {

    LocalDate checkInDate();

    LocalDate checkOutDate();
}