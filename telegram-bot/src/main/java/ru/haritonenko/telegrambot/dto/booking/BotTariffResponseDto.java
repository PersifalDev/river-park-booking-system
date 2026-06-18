package ru.haritonenko.telegrambot.dto.booking;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record BotTariffResponseDto(
        String code,
        String title,
        String description,
        BigDecimal priceAmount,
        String priceModifierType,
        BigDecimal priceModifierValue,
        String cancellationPolicy,
        Integer freeCancellationDaysBefore,
        String includedServices,
        Integer minNights,
        Integer maxNights,
        Integer minAdults,
        Integer minChildren
) {
}
