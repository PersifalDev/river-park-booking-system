package ru.haritonenko.bookingservice.domain.tariff.price;

import org.springframework.stereotype.Component;
import ru.haritonenko.bookingservice.domain.db.entity.BookingTariffEntity;
import ru.haritonenko.bookingservice.domain.tariff.TariffPriceModifierType;

import java.math.BigDecimal;

@Component
public class PercentTariffPriceModifierStrategy implements TariffPriceModifierStrategy {

    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    @Override
    public boolean supports(TariffPriceModifierType type) {
        return TariffPriceModifierType.PERCENT == type;
    }

    @Override
    public BigDecimal apply(BigDecimal basePrice, long nights, BookingTariffEntity tariff) {
        BigDecimal multiplier = BigDecimal.ONE.add(tariff.getPriceModifierValue().divide(ONE_HUNDRED));
        return basePrice.multiply(multiplier);
    }
}
