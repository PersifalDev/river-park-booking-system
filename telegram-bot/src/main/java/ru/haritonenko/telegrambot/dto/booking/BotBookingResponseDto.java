package ru.haritonenko.telegrambot.dto.booking;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BotBookingResponseDto(
        UUID id,
        String bookingCode,
        Long userId,
        Long roomCategoryId,
        Integer guests,
        Integer adultCount,
        Integer childrenCount,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        BigDecimal priceAmount,
        OffsetDateTime holdExpiresAt,
        Boolean hasPromo,
        String status,
        String cancellationReason,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
