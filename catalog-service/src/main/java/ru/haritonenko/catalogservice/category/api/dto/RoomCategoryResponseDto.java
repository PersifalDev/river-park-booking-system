package ru.haritonenko.catalogservice.category.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import ru.haritonenko.catalogservice.category.domain.type.RoomType;

import java.math.BigDecimal;

@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RoomCategoryResponseDto(
    Long id,
    RoomType name,
    String description,
    Integer maxGuests,
    BigDecimal basePrice,
    Double areaSquare,
    Integer totalUnits,
    String mainPhotoUrl
){}