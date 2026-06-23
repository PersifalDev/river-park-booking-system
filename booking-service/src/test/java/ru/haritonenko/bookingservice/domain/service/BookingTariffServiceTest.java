package ru.haritonenko.bookingservice.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import ru.haritonenko.bookingservice.api.dto.AvailableRoomSearchRequestDto;
import ru.haritonenko.bookingservice.api.dto.BookingRequestDto;
import ru.haritonenko.bookingservice.config.tariff.BookingTariffProperties;
import ru.haritonenko.bookingservice.domain.db.entity.BookingEntity;
import ru.haritonenko.bookingservice.domain.db.entity.BookingTariffEntity;
import ru.haritonenko.bookingservice.domain.db.repository.BookingTariffRepository;
import ru.haritonenko.bookingservice.domain.exception.BookingTariffNotApplicableException;
import ru.haritonenko.bookingservice.domain.tariff.TariffCancellationPolicy;
import ru.haritonenko.bookingservice.domain.tariff.TariffPriceModifierType;
import ru.haritonenko.bookingservice.domain.tariff.price.FixedPerNightTariffPriceModifierStrategy;
import ru.haritonenko.bookingservice.domain.tariff.price.FixedPerStayTariffPriceModifierStrategy;
import ru.haritonenko.bookingservice.domain.tariff.price.PercentTariffPriceModifierStrategy;
import ru.haritonenko.bookingservice.domain.tariff.rule.DateWindowTariffRule;
import ru.haritonenko.bookingservice.domain.tariff.rule.GuestCompositionTariffRule;
import ru.haritonenko.bookingservice.domain.tariff.rule.NightCountTariffRule;
import ru.haritonenko.bookingservice.external.client.catalog.CatalogServiceHttpClient;
import ru.haritonenko.commonlibs.dto.category.RoomCategoryResponseDto;
import ru.haritonenko.commonlibs.dto.category.type.RoomType;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

class BookingTariffServiceTest {

    private BookingTariffRepository tariffRepository;
    private CatalogServiceHttpClient catalogServiceHttpClient;
    private BookingPriceCalendarService priceCalendarService;
    private TransactionTemplate transactionTemplate;
    private BookingTariffService service;

    @BeforeEach
    void setUp() {
        tariffRepository = mock(BookingTariffRepository.class);
        catalogServiceHttpClient = mock(CatalogServiceHttpClient.class);
        priceCalendarService = mock(BookingPriceCalendarService.class);
        transactionTemplate = mock(TransactionTemplate.class);
        BookingTariffProperties properties = new BookingTariffProperties();
        properties.setDefaultCode("ROOM_ONLY");
        properties.setMoneyScale(2);
        properties.setPercentDenominator(100);
        properties.setRoundingMode(RoundingMode.HALF_UP);
        doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        }).when(transactionTemplate).execute(any());

        service = new BookingTariffService(
                tariffRepository,
                List.of(new NightCountTariffRule(), new GuestCompositionTariffRule(), new DateWindowTariffRule()),
                List.of(
                        new PercentTariffPriceModifierStrategy(properties),
                        new FixedPerNightTariffPriceModifierStrategy(),
                        new FixedPerStayTariffPriceModifierStrategy()
                ),
                catalogServiceHttpClient,
                properties,
                priceCalendarService,
                transactionTemplate
        );
    }

    @Test
    void shouldReturnOnlyApplicableTariffsWithCalculatedPrice() {
        BookingTariffEntity roomOnly = tariff("ROOM_ONLY", TariffPriceModifierType.PERCENT, BigDecimal.ZERO);
        BookingTariffEntity family = tariff("FAMILY", TariffPriceModifierType.FIXED_PER_STAY, BigDecimal.valueOf(2500));
        family.setMinChildren(1);
        BookingTariffEntity longStay = tariff("LONG_STAY", TariffPriceModifierType.PERCENT, BigDecimal.valueOf(-15));
        longStay.setMinNights(5);

        when(catalogServiceHttpClient.getRoomCategoryById(1L)).thenReturn(room(BigDecimal.valueOf(5000)));
        when(tariffRepository.findAllByActiveTrueOrderBySortOrderAscTitleAsc())
                .thenReturn(List.of(roomOnly, family, longStay));
        when(priceCalendarService.calculateBasePrice(
                room(BigDecimal.valueOf(5000)),
                roomOnly,
                request(3, 2, 1).checkInDate(),
                request(3, 2, 1).checkOutDate()
        )).thenReturn(BigDecimal.valueOf(15000));
        when(priceCalendarService.calculateBasePrice(
                room(BigDecimal.valueOf(5000)),
                family,
                request(3, 2, 1).checkInDate(),
                request(3, 2, 1).checkOutDate()
        )).thenReturn(BigDecimal.valueOf(15000));

        var result = service.findApplicableTariffs(request(3, 2, 1));

        assertEquals(2, result.size());
        assertEquals("ROOM_ONLY", result.get(0).code());
        assertEquals(0, BigDecimal.valueOf(15000).compareTo(result.get(0).priceAmount()));
        assertEquals("FAMILY", result.get(1).code());
        assertEquals(0, BigDecimal.valueOf(17500).compareTo(result.get(1).priceAmount()));
    }

    @Test
    void shouldUseDefaultTariffCodeWhenBookingHasNoTariff() {
        BookingTariffEntity roomOnly = tariff("ROOM_ONLY", TariffPriceModifierType.PERCENT, BigDecimal.ZERO);
        when(tariffRepository.findByCodeAndActiveTrue("ROOM_ONLY")).thenReturn(Optional.of(roomOnly));

        BookingTariffEntity actual = service.requireApplicableTariff(booking(null, 1, 2, 0));

        assertEquals("ROOM_ONLY", actual.getCode());
    }

    @Test
    void shouldRejectSelectedTariffWhenRulesDoNotApply() {
        BookingTariffEntity longStay = tariff("LONG_STAY", TariffPriceModifierType.PERCENT, BigDecimal.valueOf(-15));
        longStay.setMinNights(5);
        when(tariffRepository.findByCodeAndActiveTrue("LONG_STAY")).thenReturn(Optional.of(longStay));

        assertThrows(
                BookingTariffNotApplicableException.class,
                () -> service.requireApplicableTariff(booking("LONG_STAY", 2, 2, 0))
        );
    }

    @Test
    void shouldApplyFixedPerNightModifier() {
        BookingTariffEntity breakfast = tariff("BREAKFAST", TariffPriceModifierType.FIXED_PER_NIGHT, BigDecimal.valueOf(500));

        BigDecimal actual = service.calculateTariffPrice(BigDecimal.valueOf(10000), 2, breakfast);

        assertEquals(0, BigDecimal.valueOf(11000).compareTo(actual));
    }

    @Test
    void shouldTreatUnknownGuestCompositionAsApplicableForAvailableSearch() {
        BookingTariffEntity roomOnly = tariff("ROOM_ONLY", TariffPriceModifierType.PERCENT, BigDecimal.ZERO);
        roomOnly.setMinAdults(1);
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        AvailableRoomSearchRequestDto request = new AvailableRoomSearchRequestDto(
                checkInDate,
                checkInDate.plusDays(1),
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        when(tariffRepository.findAllByActiveTrueOrderBySortOrderAscTitleAsc()).thenReturn(List.of(roomOnly));
        when(priceCalendarService.calculateBasePrice(room(BigDecimal.valueOf(5000)), roomOnly, request.checkInDate(), request.checkOutDate()))
                .thenReturn(BigDecimal.valueOf(5000));

        assertTrue(service.hasAvailableTariff(room(BigDecimal.valueOf(5000)), request));
    }

    private BookingRequestDto request(int nights, int adults, int children) {
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        return BookingRequestDto.builder()
                .categoryId(1L)
                .checkInDate(checkInDate)
                .checkOutDate(checkInDate.plusDays(nights))
                .guests(adults + children)
                .adultCount(adults)
                .childrenCount(children)
                .build();
    }

    private BookingEntity booking(String tariffCode, int nights, int adults, int children) {
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        return BookingEntity.builder()
                .id(UUID.randomUUID())
                .tariffCode(tariffCode)
                .checkInDate(checkInDate)
                .checkOutDate(checkInDate.plusDays(nights))
                .guests(adults + children)
                .adultCount(adults)
                .childrenCount(children)
                .build();
    }

    private BookingTariffEntity tariff(String code, TariffPriceModifierType modifierType, BigDecimal modifierValue) {
        return BookingTariffEntity.builder()
                .code(code)
                .title(code)
                .priceModifierType(modifierType)
                .priceModifierValue(modifierValue)
                .cancellationPolicy(TariffCancellationPolicy.FLEXIBLE)
                .sortOrder(10)
                .active(true)
                .build();
    }

    private RoomCategoryResponseDto room(BigDecimal basePrice) {
        return new RoomCategoryResponseDto(
                1L,
                RoomType.STANDARD,
                "Standard room",
                2,
                basePrice,
                20.0,
                30,
                null,
                null
        );
    }
}
