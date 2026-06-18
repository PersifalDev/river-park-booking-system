package ru.haritonenko.bookingservice.domain.exception;

public class BookingTariffNotApplicableException extends RuntimeException {

    public BookingTariffNotApplicableException(String message) {
        super(message);
    }
}
