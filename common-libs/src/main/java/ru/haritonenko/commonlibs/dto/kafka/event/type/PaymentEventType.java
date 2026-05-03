package ru.haritonenko.commonlibs.dto.kafka.event.type;

public enum PaymentEventType {

    PAYMENT_INVOICE_CREATED,
    PAYMENT_PENDING,
    PAYMENT_CONFIRMED,
    PAYMENT_CANCELLED,
    PAYMENT_FAILED
}