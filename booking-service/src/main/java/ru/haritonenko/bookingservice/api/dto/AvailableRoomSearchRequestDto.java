package ru.haritonenko.bookingservice.api.dto;

import jakarta.validation.constraints.NotNull;
import ru.haritonenko.commonlibs.dto.category.type.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AvailableRoomSearchRequestDto(
        @NotNull LocalDate checkInDate,
        @NotNull LocalDate checkOutDate,
        Integer guests,
        RoomType roomType,
        BigDecimal priceFrom,
        BigDecimal priceTo,
        BigDecimal minArea
) {
}
