package ru.haritonenko.bookingservice.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import ru.haritonenko.bookingservice.domain.status.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record BookingResponseDto(
        UUID id,
        String bookingCode,
        Long userId,
        Long roomCategoryId,
        String roomNumberSnapshot,
        Integer guests,
        Integer adultCount,
        Integer childrenCount,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        BigDecimal priceAmount,
        String tariffCode,
        String tariffTitle,
        String tariffCancellationPolicy,
        Integer tariffFreeCancellationDaysBefore,
        String tariffIncludedServices,
        OffsetDateTime holdExpiresAt,
        Boolean hasPromo,
        String appliedPromoCode,
        String generatedPromoCode,
        Integer promoDiscountPercent,
        BookingStatus status,
        String cancellationReason,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
