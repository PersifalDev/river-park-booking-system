package ru.haritonenko.paymentservice.domain.exception;

public class IllegalPaymentStateException extends RuntimeException {
    public IllegalPaymentStateException(String message) {
        super(message);
    }
}
