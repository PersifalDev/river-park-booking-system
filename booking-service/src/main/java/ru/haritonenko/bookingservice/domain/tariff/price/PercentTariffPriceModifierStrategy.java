package ru.haritonenko.bookingservice.domain.tariff.price;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.haritonenko.bookingservice.config.tariff.BookingTariffProperties;
import ru.haritonenko.bookingservice.domain.db.entity.BookingTariffEntity;
import ru.haritonenko.bookingservice.domain.tariff.TariffPriceModifierType;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class PercentTariffPriceModifierStrategy implements TariffPriceModifierStrategy {

    private final BookingTariffProperties properties;

    @Override
    public boolean supports(TariffPriceModifierType type) {
        return TariffPriceModifierType.PERCENT == type;
    }

    @Override
    public BigDecimal apply(BigDecimal basePrice, long nights, BookingTariffEntity tariff) {
        BigDecimal multiplier = BigDecimal.ONE.add(
                tariff.getPriceModifierValue().divide(BigDecimal.valueOf(properties.getPercentDenominator()))
        );
        return basePrice.multiply(multiplier);
    }
}
