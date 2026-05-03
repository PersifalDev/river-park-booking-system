package ru.haritonenko.catalogservice.photo.category.domain.exception;

public class PhotoListNotFoundException extends RuntimeException {
    public PhotoListNotFoundException(String message) {
        super(message);
    }
}
