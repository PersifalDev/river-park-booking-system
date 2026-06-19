package ru.haritonenko.bookingservice.domain.db.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import ru.haritonenko.bookingservice.domain.db.entity.BookingPriceCalendarEntity;
import ru.haritonenko.bookingservice.domain.db.entity.BookingTariffEntity;
import ru.haritonenko.bookingservice.domain.tariff.TariffCancellationPolicy;
import ru.haritonenko.bookingservice.domain.tariff.TariffPriceModifierType;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class BookingPriceCalendarRepositoryTest {

    @Autowired
    private BookingPriceCalendarRepository repository;

    @Autowired
    private BookingTariffRepository tariffRepository;

    @Test
    void shouldFindCalendarRatesByCategoryRatePlanAndDateRange() {
        BookingTariffEntity tariff = tariffRepository.save(tariff());
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        repository.save(rate(1L, tariff, checkInDate, BigDecimal.valueOf(5000)));
        repository.save(rate(1L, tariff, checkInDate.plusDays(1), BigDecimal.valueOf(6000)));
        repository.save(rate(2L, tariff, checkInDate, BigDecimal.valueOf(7000)));
        repository.flush();

        var result = repository.findAllByRoomCategoryIdAndRatePlan_IdAndCalendarDateGreaterThanEqualAndCalendarDateLessThanOrderByCalendarDateAsc(
                1L,
                tariff.getId(),
                checkInDate,
                checkInDate.plusDays(2)
        );

        assertEquals(2, result.size());
        assertEquals(0, BigDecimal.valueOf(5000).compareTo(result.get(0).getPrice()));
        assertEquals(0, BigDecimal.valueOf(6000).compareTo(result.get(1).getPrice()));
    }

    private BookingPriceCalendarEntity rate(Long roomCategoryId, BookingTariffEntity tariff, LocalDate date, BigDecimal price) {
        return BookingPriceCalendarEntity.builder()
                .roomCategoryId(roomCategoryId)
                .calendarDate(date)
                .ratePlan(tariff)
                .price(price)
                .available(true)
                .closedToArrival(false)
                .closedToDeparture(false)
                .demandMultiplier(BigDecimal.ONE)
                .build();
    }

    private BookingTariffEntity tariff() {
        return BookingTariffEntity.builder()
                .code("ROOM_ONLY")
                .title("Без завтрака")
                .description("Base tariff")
                .priceModifierType(TariffPriceModifierType.PERCENT)
                .priceModifierValue(BigDecimal.ZERO)
                .cancellationPolicy(TariffCancellationPolicy.FLEXIBLE)
                .sortOrder(10)
                .active(true)
                .build();
    }
}
