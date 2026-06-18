package ru.haritonenko.bookingservice.domain.tariff.rule;

import org.springframework.stereotype.Component;
import ru.haritonenko.bookingservice.domain.db.entity.BookingTariffEntity;
import ru.haritonenko.bookingservice.domain.tariff.TariffSearchContext;

@Component
public class DateWindowTariffRule implements TariffApplicabilityRule {

    @Override
    public boolean applies(BookingTariffEntity tariff, TariffSearchContext context) {
        if (tariff.getActiveFrom() != null && context.checkInDate().isBefore(tariff.getActiveFrom())) {
            return false;
        }
        return tariff.getActiveTo() == null || !context.checkInDate().isAfter(tariff.getActiveTo());
    }
}
