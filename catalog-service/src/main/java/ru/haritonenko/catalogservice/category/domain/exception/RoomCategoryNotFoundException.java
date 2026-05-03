package ru.haritonenko.catalogservice.category.domain.exception;

public class RoomCategoryNotFoundException extends RuntimeException {
    public RoomCategoryNotFoundException(String message) {
        super(message);
    }
}
