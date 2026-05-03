package ru.haritonenko.telegrambot.dto.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BotPaymentResponseDto(
        UUID id,
        UUID bookingId,
        String bookingCode,
        Long userId,
        BigDecimal priceAmount,
        String status,
        String paymentMethod,
        String paymentComment,
        String contactPhone,
        String paymentInstruction,
        String cancellationReason,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
