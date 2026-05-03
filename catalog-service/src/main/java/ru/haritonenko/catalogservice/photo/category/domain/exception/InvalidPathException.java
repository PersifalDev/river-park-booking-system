package ru.haritonenko.catalogservice.photo.category.domain.exception;

public class InvalidPathException extends RuntimeException {
    public InvalidPathException(String message) {
        super(message);
    }
}
