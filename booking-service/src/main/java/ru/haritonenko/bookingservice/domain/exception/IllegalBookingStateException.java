package ru.haritonenko.bookingservice.domain.exception;

public class IllegalBookingStateException extends RuntimeException {
    public IllegalBookingStateException(String message) {
        super(message);
    }
}
