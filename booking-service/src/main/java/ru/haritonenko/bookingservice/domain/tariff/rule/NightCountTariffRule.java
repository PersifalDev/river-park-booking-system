package ru.haritonenko.bookingservice.domain.tariff.rule;

import org.springframework.stereotype.Component;
import ru.haritonenko.bookingservice.domain.db.entity.BookingTariffEntity;
import ru.haritonenko.bookingservice.domain.tariff.TariffSearchContext;

@Component
public class NightCountTariffRule implements TariffApplicabilityRule {

    @Override
    public boolean applies(BookingTariffEntity tariff, TariffSearchContext context) {
        if (tariff.getMinNights() != null && context.nights() < tariff.getMinNights()) {
            return false;
        }
        return tariff.getMaxNights() == null || context.nights() <= tariff.getMaxNights();
    }
}
