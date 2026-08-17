package ru.haritonenko.bookingservice.tasks.domain.exception;

public class IllegalArgumentAsyncBookingTaskException extends IllegalArgumentException {
    public IllegalArgumentAsyncBookingTaskException(String message) {
        super(message);
    }
}
