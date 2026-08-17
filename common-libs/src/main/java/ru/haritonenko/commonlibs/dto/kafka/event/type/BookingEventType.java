package ru.haritonenko.commonlibs.dto.kafka.event.type;

public enum BookingEventType implements EventType {
    BOOKING_CREATED,

    BOOKING_HOLD_CREATED,

    BOOKING_CONFIRMED,

    BOOKING_CANCELLED,

    BOOKING_EXPIRED,

    BOOKING_FAILED
}