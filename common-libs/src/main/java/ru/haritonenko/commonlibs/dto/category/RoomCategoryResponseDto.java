package ru.haritonenko.commonlibs.dto.category;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import ru.haritonenko.commonlibs.dto.category.type.RoomType;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RoomCategoryResponseDto(
        Long id,
        RoomType name,
        String description,
        Integer maxGuests,
        BigDecimal basePrice,
        Double areaSquare,
        Integer totalUnits,
        String mainPhotoUrl
) {
}
