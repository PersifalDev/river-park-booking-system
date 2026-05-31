package ru.haritonenko.bookingservice.domain.exception;

public class BookingIdempotencyConflictException extends RuntimeException {

    public BookingIdempotencyConflictException(String message) {
        super(message);
    }
}
