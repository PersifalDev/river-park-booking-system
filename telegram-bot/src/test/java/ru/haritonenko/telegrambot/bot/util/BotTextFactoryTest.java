package ru.haritonenko.telegrambot.bot.util;

import org.junit.jupiter.api.Test;
import ru.haritonenko.commonlibs.dto.category.RoomCategoryResponseDto;
import ru.haritonenko.commonlibs.dto.category.type.RoomType;
import ru.haritonenko.telegrambot.dto.booking.BotBookingResponseDto;
import ru.haritonenko.telegrambot.dto.payment.BotPaymentResponseDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BotTextFactoryTest {

    private final BotTextFactory factory = new BotTextFactory(null);

    @Test
    void shouldReplaceCorruptedPaymentTextInBookingCreatedMessage() {
        String message = factory.buildBookingCreatedMessage(
                booking(),
                room(),
                payment("���������������������������� Telegram.", "���������������� River Park."),
                "+79130000000"
        );

        assertTrue(message.contains("Инструкция: Подтвердите бронирование в Telegram."));
        assertTrue(message.contains("Примечание: Оплата производится в день заселения у администратора отеля River Park."));
        assertFalse(message.contains("����"));
    }

    @Test
    void shouldKeepReadablePaymentTextInBookingCreatedMessage() {
        String message = factory.buildBookingCreatedMessage(
                booking(),
                room(),
                payment("Оплатите на стойке регистрации.", "Нужен паспорт."),
                "+79130000000"
        );

        assertTrue(message.contains("Инструкция: Оплатите на стойке регистрации."));
        assertTrue(message.contains("Примечание: Нужен паспорт."));
    }

    private BotBookingResponseDto booking() {
        OffsetDateTime now = OffsetDateTime.now();
        return new BotBookingResponseDto(
                UUID.randomUUID(),
                "BK-1",
                1L,
                10L,
                2,
                2,
                0,
                LocalDate.of(2026, 7, 8),
                LocalDate.of(2026, 7, 9),
                BigDecimal.valueOf(6290),
                "STANDARD",
                "Невозвратный",
                "NON_REFUNDABLE",
                null,
                null,
                now.plusMinutes(15),
                false,
                null,
                null,
                null,
                "HOLD",
                null,
                now,
                now
        );
    }

    private RoomCategoryResponseDto room() {
        return new RoomCategoryResponseDto(
                10L,
                RoomType.STANDARD,
                null,
                2,
                BigDecimal.valueOf(6290),
                20.0,
                1,
                1,
                null
        );
    }

    private BotPaymentResponseDto payment(String instruction, String comment) {
        OffsetDateTime now = OffsetDateTime.now();
        return new BotPaymentResponseDto(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BK-1",
                1L,
                BigDecimal.valueOf(6290),
                "PENDING",
                "PAY_ON_ARRIVAL",
                comment,
                "+7 (383) 349-50-50",
                instruction,
                null,
                now,
                now
        );
    }
}
