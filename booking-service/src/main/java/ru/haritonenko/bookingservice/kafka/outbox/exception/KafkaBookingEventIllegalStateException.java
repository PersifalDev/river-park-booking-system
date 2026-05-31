package ru.haritonenko.bookingservice.kafka.outbox.exception;

public class KafkaBookingEventIllegalStateException extends IllegalStateException {
    public KafkaBookingEventIllegalStateException(String message) {
        super(message);
    }
}
