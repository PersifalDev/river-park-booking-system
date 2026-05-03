package ru.haritonenko.catalogservice.photo.category.domain.exception;

public class PhotoNotFoundException extends RuntimeException {
    public PhotoNotFoundException(String message) {
        super(message);
    }
}
