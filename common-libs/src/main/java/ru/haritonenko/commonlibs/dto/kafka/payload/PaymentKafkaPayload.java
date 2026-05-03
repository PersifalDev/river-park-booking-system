package ru.haritonenko.commonlibs.dto.kafka.payload;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record PaymentKafkaPayload(
        UUID bookingId,
        String bookingCode,
        UUID paymentId,
        Long userId,
        BigDecimal priceAmount,
        String paymentStatus,
        String paymentMethod,
        String paymentComment,
        String contactPhone,
        String paymentInstruction,
        String cancellationReason,
        OffsetDateTime createdAt
) {
}