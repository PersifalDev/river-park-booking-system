package ru.haritonenko.telegrambot.dto.booking;

import java.time.LocalDate;

public record BotBookingRequestDto(
        Long categoryId,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Integer guests,
        Integer adultCount,
        Integer childrenCount,
        String promoCode
) {
}
