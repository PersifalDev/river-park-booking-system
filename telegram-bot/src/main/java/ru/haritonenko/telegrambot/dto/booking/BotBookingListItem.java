package ru.haritonenko.telegrambot.dto.booking;

import java.util.UUID;

public record BotBookingListItem(
        UUID bookingId,
        String label
) {
}
