package ru.haritonenko.bookingservice.kafka.outbox.exception;

public class KafkaEventNotFoundException extends RuntimeException {
    public KafkaEventNotFoundException(String message) {
        super(message);
    }
}
