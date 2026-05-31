package ru.haritonenko.bookingservice.domain.exception;

public class IllegalBookingStateException extends IllegalStateException {
    public IllegalBookingStateException(String message) {
        super(message);
    }
}
