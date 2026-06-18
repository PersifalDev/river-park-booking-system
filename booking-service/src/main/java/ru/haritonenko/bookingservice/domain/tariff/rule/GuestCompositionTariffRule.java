package ru.haritonenko.bookingservice.domain.tariff.rule;

import org.springframework.stereotype.Component;
import ru.haritonenko.bookingservice.domain.db.entity.BookingTariffEntity;
import ru.haritonenko.bookingservice.domain.tariff.TariffSearchContext;

@Component
public class GuestCompositionTariffRule implements TariffApplicabilityRule {

    @Override
    public boolean applies(BookingTariffEntity tariff, TariffSearchContext context) {
        if (tariff.getMinAdults() != null && safe(context.adultCount()) < tariff.getMinAdults()) {
            return false;
        }
        return tariff.getMinChildren() == null || safe(context.childrenCount()) >= tariff.getMinChildren();
    }

    private int safe(Integer value) {
        return value == null ? 0 : value;
    }
}
