package ru.haritonenko.commonlibs.dto.category;

import java.math.BigDecimal;

public record RoomCategoryShortResponseDto(
        Long id,
        String name,
        String description,
        Integer maxGuests,
        BigDecimal basePrice,
        Integer totalUnits
) {
}
