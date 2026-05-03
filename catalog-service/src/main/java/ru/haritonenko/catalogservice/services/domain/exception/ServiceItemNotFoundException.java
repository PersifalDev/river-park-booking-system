package ru.haritonenko.catalogservice.services.domain.exception;

public class ServiceItemNotFoundException extends RuntimeException {

    public ServiceItemNotFoundException(Long id) {
        super("Service item was not found by id=" + id);
    }

    public ServiceItemNotFoundException(String type) {
        super("Service item was not found by type=" + type);
    }
}