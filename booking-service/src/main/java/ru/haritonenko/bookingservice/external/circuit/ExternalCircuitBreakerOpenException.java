package ru.haritonenko.bookingservice.external.circuit;

public class ExternalCircuitBreakerOpenException extends RuntimeException {

    public ExternalCircuitBreakerOpenException(String serviceName) {
        super("External service is temporarily unavailable: " + serviceName);
    }
}
