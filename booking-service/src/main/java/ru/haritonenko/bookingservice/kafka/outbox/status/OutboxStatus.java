package ru.haritonenko.bookingservice.kafka.outbox.status;

public enum OutboxStatus {
    NEW,
    PROCESSING,
    SENT,
    FAILED
}