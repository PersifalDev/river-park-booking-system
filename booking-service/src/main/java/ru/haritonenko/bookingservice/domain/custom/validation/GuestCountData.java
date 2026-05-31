package ru.haritonenko.bookingservice.domain.custom.validation;

public interface GuestCountData {

    default Integer guests() {
        return null;
    }

    default Integer adultCount() {
        return null;
    }

    default Integer childrenCount() {
        return null;
    }
}