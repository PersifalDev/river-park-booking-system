package ru.haritonenko.bookingservice.domain.tariff.price;

import ru.haritonenko.bookingservice.domain.db.entity.BookingTariffEntity;
import ru.haritonenko.bookingservice.domain.tariff.TariffPriceModifierType;

import java.math.BigDecimal;

public interface TariffPriceModifierStrategy {

    boolean supports(TariffPriceModifierType type);

    BigDecimal apply(BigDecimal basePrice, long nights, BookingTariffEntity tariff);
}
