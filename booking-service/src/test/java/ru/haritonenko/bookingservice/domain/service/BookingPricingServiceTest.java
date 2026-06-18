package ru.haritonenko.bookingservice.domain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.haritonenko.bookingservice.domain.db.entity.BookingEntity;
import ru.haritonenko.bookingservice.domain.db.entity.BookingTariffEntity;
import ru.haritonenko.bookingservice.domain.tariff.TariffCancellationPolicy;
import ru.haritonenko.bookingservice.domain.tariff.TariffPriceModifierType;
import ru.haritonenko.bookingservice.external.client.catalog.CatalogServiceHttpClient;
import ru.haritonenko.commonlibs.dto.category.RoomCategoryResponseDto;
import ru.haritonenko.commonlibs.dto.category.type.RoomType;
import ru.haritonenko.commonlibs.exception.RoomCategoryNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingPricingServiceTest {

    @Mock
    private CatalogServiceHttpClient catalogServiceHttpClient;

    @Mock
    private PromoCodeService promoCodeService;

    @Mock
    private BookingTariffService bookingTariffService;

    @InjectMocks
    private BookingPricingService bookingPricingService;

    @Test
    void shouldCalculatePriceForNightsAndApplyPromo() {
        BookingEntity booking = bookingEntity();
        BookingTariffEntity tariff = roomOnlyTariff();
        when(catalogServiceHttpClient.getRoomCategoryById(1L)).thenReturn(room(BigDecimal.valueOf(5000)));
        when(bookingTariffService.requireApplicableTariff(booking)).thenReturn(tariff);
        when(bookingTariffService.calculateTariffPrice(BigDecimal.valueOf(15000), 3, tariff))
                .thenReturn(BigDecimal.valueOf(15000));
        when(promoCodeService.applyPromoIfPresent(
                booking.getId(),
                booking.getUserId(),
                booking.getAppliedPromoCode(),
                BigDecimal.valueOf(15000)
        )).thenReturn(BigDecimal.valueOf(13500));

        BigDecimal actual = bookingPricingService.calculatePrice(booking);

        assertEquals(0, BigDecimal.valueOf(13500).compareTo(actual));
    }

    @Test
    void shouldRejectNullCategoryId() {
        BookingEntity booking = bookingEntity();
        booking.setRoomCategoryId(null);

        assertThrows(IllegalArgumentException.class, () -> bookingPricingService.calculatePrice(booking));
    }

    @Test
    void shouldRejectMissingCategory() {
        BookingEntity booking = bookingEntity();
        when(catalogServiceHttpClient.getRoomCategoryById(1L)).thenReturn(null);

        assertThrows(RoomCategoryNotFoundException.class, () -> bookingPricingService.calculatePrice(booking));
    }

    private BookingEntity bookingEntity() {
        return BookingEntity.builder()
                .id(UUID.randomUUID())
                .userId(10L)
                .roomCategoryId(1L)
                .checkInDate(LocalDate.now().plusDays(1))
                .checkOutDate(LocalDate.now().plusDays(4))
                .appliedPromoCode("PROMO10")
                .build();
    }

    private BookingTariffEntity roomOnlyTariff() {
        return BookingTariffEntity.builder()
                .code("ROOM_ONLY")
                .title("Без завтрака")
                .priceModifierType(TariffPriceModifierType.FIXED_PER_STAY)
                .priceModifierValue(BigDecimal.ZERO)
                .cancellationPolicy(TariffCancellationPolicy.FLEXIBLE)
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
