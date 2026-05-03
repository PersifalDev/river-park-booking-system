package ru.haritonenko.commonlibs.dto.kafka.payload;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Builder
public record CatalogKafkaPayload(
        Long roomCategoryId,
        String roomType,
        String categoryName,
        Integer maxGuests,
        BigDecimal basePrice,
        Integer totalUnits,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Boolean available,
        String description
) {
}