package ru.haritonenko.paymentservice.outbox;

public enum PaymentOutboxStatus {
    NEW,
    PROCESSING,
    SENT,
    FAILED
}
