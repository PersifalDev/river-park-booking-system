package ru.haritonenko.catalogservice.photo.service.domain.exception;

public class ServiceItemPhotoNotFoundException extends RuntimeException {

    public ServiceItemPhotoNotFoundException(Long serviceItemId) {
        super("Service photo was not found by serviceItemId=" + serviceItemId);
    }
}
