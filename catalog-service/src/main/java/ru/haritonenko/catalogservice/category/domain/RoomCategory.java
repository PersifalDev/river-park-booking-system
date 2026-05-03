package ru.haritonenko.catalogservice.category.domain;

import lombok.Builder;
import ru.haritonenko.catalogservice.category.domain.type.RoomType;

import java.math.BigDecimal;

@Builder(toBuilder = true)
public record RoomCategory(
        Long id,
        RoomType name,
        String description,
        Integer maxGuests,
        BigDecimal basePrice,
        Double areaSquare,
        Integer totalUnits,
        String mainPhotoPath
) {
}
