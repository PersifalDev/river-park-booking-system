package ru.haritonenko.bookingservice.domain;

import ru.haritonenko.bookingservice.domain.status.BookingStatus;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record Booking(
        UUID id,
        Long userId,
        Long roomCategoryId,
        String bookingCode,
        Integer guests,
        Integer adultCount,
        Integer childrenCount,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        BigDecimal priceAmount,
        OffsetDateTime holdExpiresAt,
        Boolean hasPromo,
        BookingStatus status,
        String cancellationReason,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
