package ru.haritonenko.bookingservice.domain;

import java.util.UUID;

public record BookingCreationResult(
        UUID bookingId,
        Long taskId
) {
}