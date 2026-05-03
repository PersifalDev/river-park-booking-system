package ru.haritonenko.commonlibs.dto.kafka.payload;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Builder
public record BookingKafkaPayload(
        UUID bookingId,
        String bookingCode,
        Long userId,
        Long roomCategoryId,
        Integer guests,
        Integer adultCount,
        Integer childrenCount,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        BigDecimal priceAmount,
        String bookingStatus,
        OffsetDateTime holdExpiresAt,
        String cancellationReason
) {
}