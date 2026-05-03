package ru.haritonenko.telegrambot.bot.state;

import lombok.Builder;
import ru.haritonenko.commonlibs.dto.category.type.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder(toBuilder = true)
public record AvailableRoomSearchDraft(
        Integer guests,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        RoomType roomType,
        BigDecimal priceFrom,
        BigDecimal priceTo,
        BigDecimal minArea
) {
    public static AvailableRoomSearchDraft empty() {
        return AvailableRoomSearchDraft.builder().build();
    }
}
