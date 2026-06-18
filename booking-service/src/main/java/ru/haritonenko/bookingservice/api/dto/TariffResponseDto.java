package ru.haritonenko.bookingservice.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import ru.haritonenko.bookingservice.domain.tariff.TariffCancellationPolicy;
import ru.haritonenko.bookingservice.domain.tariff.TariffPriceModifierType;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record TariffResponseDto(
        String code,
        String title,
        String description,
        BigDecimal priceAmount,
        TariffPriceModifierType priceModifierType,
        BigDecimal priceModifierValue,
        TariffCancellationPolicy cancellationPolicy,
        Integer freeCancellationDaysBefore,
        String includedServices,
        Integer minNights,
        Integer maxNights,
        Integer minAdults,
        Integer minChildren
) {
}
