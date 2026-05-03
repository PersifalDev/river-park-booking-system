package ru.haritonenko.catalogservice.category.api.dto.filter;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import lombok.Builder;
import ru.haritonenko.catalogservice.category.domain.type.RoomType;

import java.math.BigDecimal;

@Builder(toBuilder = true)
public record RoomCategorySearchRequestDto(
        @Min(value = 0, message = "Min count of guests is 0")
        Integer guests,
        RoomType roomType,
        @DecimalMin(value = "0.00", message = " Initial price might be greater than or equal to 0")
        BigDecimal priceFrom,
        @DecimalMin(value = "0.00", message = "Final price might be greater than or equal to 0")
        BigDecimal priceTo,
        @DecimalMin(value = "0.00", message = "Area square value might be greater than or equal to 0")
        BigDecimal minArea
) {
}
