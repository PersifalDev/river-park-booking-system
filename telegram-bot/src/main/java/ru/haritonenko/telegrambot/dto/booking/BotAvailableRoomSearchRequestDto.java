package ru.haritonenko.telegrambot.dto.booking;

import ru.haritonenko.commonlibs.dto.category.type.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BotAvailableRoomSearchRequestDto(
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Integer guests,
        RoomType roomType,
        BigDecimal priceFrom,
        BigDecimal priceTo,
        BigDecimal minArea
) {
}
