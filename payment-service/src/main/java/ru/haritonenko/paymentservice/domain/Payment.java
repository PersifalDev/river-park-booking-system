package ru.haritonenko.paymentservice.domain;

import ru.haritonenko.paymentservice.domain.status.PaymentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record Payment(
        UUID id,
        UUID bookingId,
        String bookingCode,
        Long userId,
        BigDecimal priceAmount,
        PaymentStatus status,
        String paymentMethod,
        String paymentComment,
        String contactPhone,
        String paymentInstruction,
        String cancellationReason,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
