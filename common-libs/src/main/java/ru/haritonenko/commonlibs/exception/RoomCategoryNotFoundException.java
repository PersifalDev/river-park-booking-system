package ru.haritonenko.commonlibs.exception;

public class RoomCategoryNotFoundException extends RuntimeException {
    public RoomCategoryNotFoundException(String message) {
        super(message);
    }
}
