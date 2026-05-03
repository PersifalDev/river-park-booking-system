package ru.haritonenko.bookingservice.domain.exception;

public class BookingHoldFailedException extends RuntimeException {
    public BookingHoldFailedException(String message) {
        super(message);
    }
}
