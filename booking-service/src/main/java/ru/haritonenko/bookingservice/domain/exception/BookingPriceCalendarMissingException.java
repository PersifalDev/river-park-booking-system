package ru.haritonenko.bookingservice.domain.exception;

public class BookingPriceCalendarMissingException extends RuntimeException {

    public BookingPriceCalendarMissingException(String message) {
        super(message);
    }
}
