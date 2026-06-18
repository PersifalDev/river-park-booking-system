package ru.haritonenko.bookingservice.domain.tariff.rule;

import ru.haritonenko.bookingservice.domain.db.entity.BookingTariffEntity;
import ru.haritonenko.bookingservice.domain.tariff.TariffSearchContext;

public interface TariffApplicabilityRule {

    boolean applies(BookingTariffEntity tariff, TariffSearchContext context);
}
