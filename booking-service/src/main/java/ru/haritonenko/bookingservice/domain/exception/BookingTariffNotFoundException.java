package ru.haritonenko.bookingservice.domain.exception;

public class BookingTariffNotFoundException extends RuntimeException {

    public BookingTariffNotFoundException(String message) {
        super(message);
    }
}
