package ru.haritonenko.bookingservice.domain.tariff.price;

import org.springframework.stereotype.Component;
import ru.haritonenko.bookingservice.domain.db.entity.BookingTariffEntity;
import ru.haritonenko.bookingservice.domain.tariff.TariffPriceModifierType;

import java.math.BigDecimal;

@Component
public class FixedPerStayTariffPriceModifierStrategy implements TariffPriceModifierStrategy {

    @Override
    public boolean supports(TariffPriceModifierType type) {
        return TariffPriceModifierType.FIXED_PER_STAY == type;
    }

    @Override
    public BigDecimal apply(BigDecimal basePrice, long nights, BookingTariffEntity tariff) {
        return basePrice.add(tariff.getPriceModifierValue());
    }
}
