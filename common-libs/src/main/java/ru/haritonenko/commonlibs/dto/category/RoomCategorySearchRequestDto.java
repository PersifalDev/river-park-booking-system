package ru.haritonenko.commonlibs.dto.category;

import lombok.Builder;
import ru.haritonenko.commonlibs.dto.category.type.RoomType;

import java.math.BigDecimal;

@Builder(toBuilder = true)
public record RoomCategorySearchRequestDto(
        Integer guests,
        RoomType roomType,
        BigDecimal priceFrom,
        BigDecimal priceTo,
        BigDecimal minArea
) {
}
