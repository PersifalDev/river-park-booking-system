package ru.haritonenko.bookingservice.domain.exception;

public class BookingAvailabilityException extends RuntimeException {
    public BookingAvailabilityException(String message) {
        super(message);
    }
}
