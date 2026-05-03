package ru.haritonenko.telegrambot.bot.state;

import lombok.Builder;

import java.time.LocalDate;

@Builder(toBuilder = true)
public record BookingDraft(
        Long roomCategoryId,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Integer adultCount,
        Integer childrenCount,
        String promoCode
) {

    public static BookingDraft empty() {
        return BookingDraft.builder().build();
    }
}
