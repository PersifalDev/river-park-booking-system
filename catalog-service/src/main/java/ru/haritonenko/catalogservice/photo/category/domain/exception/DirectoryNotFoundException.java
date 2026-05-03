package ru.haritonenko.catalogservice.photo.category.domain.exception;

public class DirectoryNotFoundException extends RuntimeException {
    public DirectoryNotFoundException(String message) {
        super(message);
    }
}
