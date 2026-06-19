package ru.haritonenko.bookingservice.domain.exception;

public class BookingPriceCalendarUnavailableException extends RuntimeException {

    public BookingPriceCalendarUnavailableException(String message) {
        super(message);
    }
}
