package ru.haritonenko.bookingservice.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.haritonenko.bookingservice.config.pricing.BookingPriceCalendarProperties;
import ru.haritonenko.bookingservice.domain.db.entity.BookingPriceCalendarEntity;
import ru.haritonenko.bookingservice.domain.db.entity.BookingTariffEntity;
import ru.haritonenko.bookingservice.domain.db.repository.BookingPriceCalendarRepository;
import ru.haritonenko.bookingservice.domain.exception.BookingPriceCalendarMissingException;
import ru.haritonenko.bookingservice.domain.exception.BookingPriceCalendarUnavailableException;
import ru.haritonenko.commonlibs.dto.category.RoomCategoryResponseDto;
import ru.haritonenko.commonlibs.dto.category.type.RoomType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BookingPriceCalendarServiceTest {

    private BookingPriceCalendarRepository repository;
    private BookingPriceCalendarProperties properties;
    private BookingPriceCalendarService service;

    @BeforeEach
    void setUp() {
        repository = mock(BookingPriceCalendarRepository.class);
        properties = new BookingPriceCalendarProperties();
        properties.setMoneyScale(2);
        properties.setRoundingMode(RoundingMode.HALF_UP);
        properties.setFallbackToCategoryBasePrice(true);
        service = new BookingPriceCalendarService(repository, properties);
    }

    @Test
    void shouldCalculateBasePriceFromNightlyCalendarRates() {
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        BookingTariffEntity tariff = tariff();
        when(repository.findAllByRoomCategoryIdAndRatePlan_IdAndCalendarDateGreaterThanEqualAndCalendarDateLessThanOrderByCalendarDateAsc(
                1L,
                10L,
                checkInDate,
                checkInDate.plusDays(3)
        )).thenReturn(List.of(
                rate(checkInDate, BigDecimal.valueOf(5000), BigDecimal.ONE),
                rate(checkInDate.plusDays(1), BigDecimal.valueOf(6000), BigDecimal.valueOf(1.1000)),
                rate(checkInDate.plusDays(2), BigDecimal.valueOf(7000), BigDecimal.ONE)
        ));

        BigDecimal actual = service.calculateBasePrice(room(), tariff, checkInDate, checkInDate.plusDays(3));

        assertEquals(0, BigDecimal.valueOf(18600).compareTo(actual));
    }

    @Test
    void shouldFallbackToCategoryBasePriceWhenCalendarRateMissingAndFallbackEnabled() {
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        BookingTariffEntity tariff = tariff();
        when(repository.findAllByRoomCategoryIdAndRatePlan_IdAndCalendarDateGreaterThanEqualAndCalendarDateLessThanOrderByCalendarDateAsc(
                1L,
                10L,
                checkInDate,
                checkInDate.plusDays(2)
        )).thenReturn(List.of(rate(checkInDate, BigDecimal.valueOf(5000), BigDecimal.ONE)));

        BigDecimal actual = service.calculateBasePrice(room(), tariff, checkInDate, checkInDate.plusDays(2));

        assertEquals(0, BigDecimal.valueOf(9500).compareTo(actual));
    }

    @Test
    void shouldRejectMissingRateWhenFallbackDisabled() {
        properties.setFallbackToCategoryBasePrice(false);
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        BookingTariffEntity tariff = tariff();
        when(repository.findAllByRoomCategoryIdAndRatePlan_IdAndCalendarDateGreaterThanEqualAndCalendarDateLessThanOrderByCalendarDateAsc(
                1L,
                10L,
                checkInDate,
                checkInDate.plusDays(1)
        )).thenReturn(List.of());

        assertThrows(
                BookingPriceCalendarMissingException.class,
                () -> service.calculateBasePrice(room(), tariff, checkInDate, checkInDate.plusDays(1))
        );
    }

    @Test
    void shouldRejectUnavailableRate() {
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        BookingPriceCalendarEntity rate = rate(checkInDate, BigDecimal.valueOf(5000), BigDecimal.ONE);
        rate.setAvailable(false);
        when(repository.findAllByRoomCategoryIdAndRatePlan_IdAndCalendarDateGreaterThanEqualAndCalendarDateLessThanOrderByCalendarDateAsc(
                1L,
                10L,
                checkInDate,
                checkInDate.plusDays(1)
        )).thenReturn(List.of(rate));

        assertThrows(
                BookingPriceCalendarUnavailableException.class,
                () -> service.calculateBasePrice(room(), tariff(), checkInDate, checkInDate.plusDays(1))
        );
    }

    @Test
    void shouldRejectClosedToArrivalAndMinStay() {
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        BookingPriceCalendarEntity rate = rate(checkInDate, BigDecimal.valueOf(5000), BigDecimal.ONE);
        rate.setClosedToArrival(true);
        rate.setMinStay(2);
        when(repository.findAllByRoomCategoryIdAndRatePlan_IdAndCalendarDateGreaterThanEqualAndCalendarDateLessThanOrderByCalendarDateAsc(
                1L,
                10L,
                checkInDate,
                checkInDate.plusDays(1)
        )).thenReturn(List.of(rate));

        assertThrows(
                BookingPriceCalendarUnavailableException.class,
                () -> service.calculateBasePrice(room(), tariff(), checkInDate, checkInDate.plusDays(1))
        );
    }

    private BookingPriceCalendarEntity rate(LocalDate date, BigDecimal price, BigDecimal multiplier) {
        return BookingPriceCalendarEntity.builder()
                .roomCategoryId(1L)
                .calendarDate(date)
                .ratePlan(tariff())
                .price(price)
                .available(true)
                .closedToArrival(false)
                .closedToDeparture(false)
                .demandMultiplier(multiplier)
                .build();
    }

    private BookingTariffEntity tariff() {
        return BookingTariffEntity.builder()
                .id(10L)
                .code("ROOM_ONLY")
                .build();
    }

    private RoomCategoryResponseDto room() {
        return new RoomCategoryResponseDto(
                1L,
                RoomType.STANDARD,
                "Standard room",
                2,
                BigDecimal.valueOf(4500),
                20.0,
                30,
                null,
                null
        );
    }
}
