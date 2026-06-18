package ru.haritonenko.bookingservice.domain.db.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import ru.haritonenko.bookingservice.domain.db.entity.BookingTariffEntity;
import ru.haritonenko.bookingservice.domain.tariff.TariffCancellationPolicy;
import ru.haritonenko.bookingservice.domain.tariff.TariffPriceModifierType;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class BookingTariffRepositoryTest {

    @Autowired
    private BookingTariffRepository repository;

    @Test
    void shouldFindOnlyActiveTariffByCode() {
        repository.save(tariff("ROOM_ONLY", true, 20));
        repository.save(tariff("BREAKFAST", false, 10));
        repository.flush();

        assertTrue(repository.findByCodeAndActiveTrue("ROOM_ONLY").isPresent());
        assertTrue(repository.findByCodeAndActiveTrue("BREAKFAST").isEmpty());
    }

    @Test
    void shouldOrderActiveTariffsBySortOrderAndTitle() {
        repository.save(tariff("B", "С завтраком", true, 20));
        repository.save(tariff("A", "Аквапарк", true, 10));
        repository.save(tariff("C", "Неактивный", false, 1));
        repository.flush();

        var result = repository.findAllByActiveTrueOrderBySortOrderAscTitleAsc();

        assertEquals(2, result.size());
        assertEquals("A", result.get(0).getCode());
        assertEquals("B", result.get(1).getCode());
    }

    private BookingTariffEntity tariff(String code, boolean active, int sortOrder) {
        return tariff(code, code, active, sortOrder);
    }

    private BookingTariffEntity tariff(String code, String title, boolean active, int sortOrder) {
        return BookingTariffEntity.builder()
                .code(code)
                .title(title)
                .description("description")
                .priceModifierType(TariffPriceModifierType.PERCENT)
                .priceModifierValue(BigDecimal.ZERO)
                .cancellationPolicy(TariffCancellationPolicy.FLEXIBLE)
                .sortOrder(sortOrder)
                .active(active)
                .build();
    }
}
