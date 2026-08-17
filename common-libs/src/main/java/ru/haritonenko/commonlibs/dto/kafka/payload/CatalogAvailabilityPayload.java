package ru.haritonenko.commonlibs.dto.kafka.payload;

import lombok.Builder;

import java.time.LocalDate;

@Builder
public record CatalogAvailabilityPayload(
        Long roomCategoryId,
        LocalDate checkInDate,
        LocalDate checkOutDate,
        Integer requestedUnits,
        Integer totalUnits,
        Boolean available
) {
}