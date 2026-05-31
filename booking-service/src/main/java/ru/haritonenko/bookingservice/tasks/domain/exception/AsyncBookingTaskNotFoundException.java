package ru.haritonenko.bookingservice.tasks.domain.exception;

public class AsyncBookingTaskNotFoundException extends RuntimeException {
    public AsyncBookingTaskNotFoundException(String message) {
        super(message);
    }
}
