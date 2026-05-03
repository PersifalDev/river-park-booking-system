package ru.haritonenko.commonlibs.exception;

public class BookingGuestsOverloadedException extends RuntimeException {
    public BookingGuestsOverloadedException(String message) {
        super(message);
    }
}
