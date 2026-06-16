package ru.haritonenko.bookingservice.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ru.haritonenko.bookingservice.domain.db.entity.BookingEntity;
import ru.haritonenko.bookingservice.domain.db.entity.PromoCodeEntity;
import ru.haritonenko.bookingservice.domain.db.repository.BookingEntityRepository;
import ru.haritonenko.bookingservice.domain.db.repository.PromoCodeRepository;
import ru.haritonenko.bookingservice.domain.exception.IllegalBookingStateException;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PromoCodeServiceTest {

    private final PromoCodeRepository promoCodeRepository = mock(PromoCodeRepository.class);
    private final BookingEntityRepository bookingRepository = mock(BookingEntityRepository.class);

    private final PromoCodeService service = new PromoCodeService(promoCodeRepository, bookingRepository);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "generatedDiscountPercent", 10);
        when(promoCodeRepository.save(any(PromoCodeEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldGeneratePromoCodeForBooking() {
        BookingEntity booking = booking(UUID.randomUUID());

        String code = service.generateForBooking(booking);

        assertNotNull(code);
        assertTrue(code.startsWith("RP-"));
        assertEquals(code, booking.getGeneratedPromoCode());
        assertEquals(10, booking.getPromoDiscountPercent());
        verify(promoCodeRepository).save(any(PromoCodeEntity.class));
    }

    @Test
    void shouldReturnExistingGeneratedPromoCode() {
        BookingEntity booking = booking(UUID.randomUUID());
        booking.setGeneratedPromoCode("RP-EXISTING");

        assertEquals("RP-EXISTING", service.generateForBooking(booking));
    }

    @Test
    void shouldApplyUsablePromoCode() {
        UUID bookingId = UUID.randomUUID();
        BookingEntity booking = booking(bookingId);
        PromoCodeEntity promo = PromoCodeEntity.builder()
                .code("RP-TEST")
                .userId(10L)
                .sourceBookingId(UUID.randomUUID())
                .discountPercent(15)
                .used(false)
                .build();
        when(promoCodeRepository.findForUpdateByCodeAndUserIdAndUsedFalse("RP-TEST", 10L))
                .thenReturn(Optional.of(promo));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        BigDecimal actual = service.applyPromoIfPresent(bookingId, 10L, " rp-test ", BigDecimal.valueOf(10000));

        assertEquals(0, BigDecimal.valueOf(8500).setScale(2).compareTo(actual));
        assertTrue(promo.getUsed());
        assertEquals(bookingId, promo.getRedeemedBookingId());
        assertTrue(booking.getHasPromo());
        assertEquals("RP-TEST", booking.getAppliedPromoCode());
        assertEquals(15, booking.getPromoDiscountPercent());
    }

    @Test
    void shouldIgnoreInvalidPromoCodeAndClearBookingPromoFields() {
        UUID bookingId = UUID.randomUUID();
        BookingEntity booking = booking(bookingId);
        booking.setHasPromo(true);
        booking.setAppliedPromoCode("BAD");
        booking.setPromoDiscountPercent(10);
        when(promoCodeRepository.findForUpdateByCodeAndUserIdAndUsedFalse("BAD", 10L))
                .thenReturn(Optional.empty());
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        BigDecimal actual = service.applyPromoIfPresent(bookingId, 10L, "BAD", BigDecimal.valueOf(10000));

        assertEquals(0, BigDecimal.valueOf(10000).compareTo(actual));
        assertFalse(booking.getHasPromo());
        assertEquals(null, booking.getAppliedPromoCode());
        assertEquals(null, booking.getPromoDiscountPercent());
    }

    @Test
    void shouldIgnorePromoGeneratedBySameBooking() {
        UUID bookingId = UUID.randomUUID();
        BookingEntity booking = booking(bookingId);
        PromoCodeEntity promo = PromoCodeEntity.builder()
                .code("RP-SELF")
                .userId(10L)
                .sourceBookingId(bookingId)
                .discountPercent(10)
                .used(false)
                .build();
        when(promoCodeRepository.findForUpdateByCodeAndUserIdAndUsedFalse("RP-SELF", 10L))
                .thenReturn(Optional.of(promo));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.of(booking));

        BigDecimal actual = service.applyPromoIfPresent(bookingId, 10L, "RP-SELF", BigDecimal.valueOf(10000));

        assertEquals(0, BigDecimal.valueOf(10000).compareTo(actual));
        assertFalse(Boolean.TRUE.equals(promo.getUsed()));
    }

    @Test
    void shouldRejectApplyingPromoWhenBookingMissing() {
        UUID bookingId = UUID.randomUUID();
        PromoCodeEntity promo = PromoCodeEntity.builder()
                .code("RP-TEST")
                .userId(10L)
                .sourceBookingId(UUID.randomUUID())
                .discountPercent(10)
                .used(false)
                .build();
        when(promoCodeRepository.findForUpdateByCodeAndUserIdAndUsedFalse("RP-TEST", 10L))
                .thenReturn(Optional.of(promo));
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        assertThrows(
                IllegalBookingStateException.class,
                () -> service.applyPromoIfPresent(bookingId, 10L, "RP-TEST", BigDecimal.valueOf(10000))
        );
    }

    @Test
    void shouldCheckPromoUsabilityForUser() {
        when(promoCodeRepository.findByCode("RP-TEST")).thenReturn(Optional.of(PromoCodeEntity.builder()
                .code("RP-TEST")
                .userId(10L)
                .used(false)
                .build()));

        assertTrue(service.isPromoCodeUsableForUser(" rp-test ", 10L));
        assertFalse(service.isPromoCodeUsableForUser(" rp-test ", 11L));
        assertTrue(service.isPromoCodeUsableForUser(null, 11L));
    }

    private BookingEntity booking(UUID id) {
        return BookingEntity.builder()
                .id(id)
                .userId(10L)
                .hasPromo(false)
                .build();
    }
}
