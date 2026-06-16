package ru.haritonenko.bookingservice.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.haritonenko.bookingservice.domain.db.entity.BookingEntity;
import ru.haritonenko.bookingservice.domain.db.entity.PromoCodeEntity;
import ru.haritonenko.bookingservice.domain.db.repository.BookingEntityRepository;
import ru.haritonenko.bookingservice.domain.db.repository.PromoCodeRepository;
import ru.haritonenko.bookingservice.domain.exception.IllegalBookingStateException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class PromoCodeService {

    private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);
    private static final int MAX_GENERATION_ATTEMPTS = 16;

    private final PromoCodeRepository promoCodeRepository;
    private final BookingEntityRepository bookingRepository;

    @Value("${app.booking.promo.generated-discount-percent:10}")
    private int generatedDiscountPercent;

    @Transactional
    public String generateForBooking(BookingEntity booking) {
        if (booking.getGeneratedPromoCode() != null && !booking.getGeneratedPromoCode().isBlank()) {
            return booking.getGeneratedPromoCode();
        }
        if (promoCodeRepository.existsBySourceBookingId(booking.getId())) {
            return booking.getGeneratedPromoCode();
        }

        String code = generateUniqueCode();
        promoCodeRepository.save(PromoCodeEntity.builder()
                .code(code)
                .userId(booking.getUserId())
                .sourceBookingId(booking.getId())
                .discountPercent(generatedDiscountPercent)
                .used(false)
                .build());
        booking.setGeneratedPromoCode(code);
        if (booking.getPromoDiscountPercent() == null) {
            booking.setPromoDiscountPercent(generatedDiscountPercent);
        }
        return code;
    }

    @Transactional
    public BigDecimal applyPromoIfPresent(UUID bookingId, Long userId, String appliedPromoCode, BigDecimal priceAmount) {
        String promoCode = normalize(appliedPromoCode);
        if (promoCode == null) {
            return priceAmount;
        }

        PromoCodeEntity promoCodeEntity = promoCodeRepository
                .findForUpdateByCodeAndUserIdAndUsedFalse(promoCode, userId)
                .orElse(null);
        if (promoCodeEntity == null) {
            log.warn("Promo code is invalid or already used, booking continues without discount: bookingId={}, userId={}",
                    bookingId, userId);
            clearAppliedPromo(bookingId);
            return priceAmount;
        }

        if (bookingId.equals(promoCodeEntity.getSourceBookingId())) {
            log.warn("Promo code can not be used for the booking that generated it, booking continues without discount: bookingId={}, userId={}",
                    bookingId, userId);
            clearAppliedPromo(bookingId);
            return priceAmount;
        }

        promoCodeEntity.setUsed(true);
        promoCodeEntity.setRedeemedBookingId(bookingId);
        promoCodeEntity.setRedeemedAt(OffsetDateTime.now());
        BookingEntity booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new IllegalBookingStateException("Booking not found for promo applying id=%s".formatted(bookingId)));
        booking.setHasPromo(true);
        booking.setAppliedPromoCode(promoCode);
        booking.setPromoDiscountPercent(promoCodeEntity.getDiscountPercent());

        BigDecimal discount = HUNDRED.subtract(BigDecimal.valueOf(promoCodeEntity.getDiscountPercent()))
                .divide(HUNDRED, 4, RoundingMode.HALF_UP);
        return priceAmount.multiply(discount).setScale(2, RoundingMode.HALF_UP);
    }

    public boolean isPromoCodeUsableForUser(String promoCode, Long userId) {
        String normalized = normalize(promoCode);
        return normalized == null || promoCodeRepository.findByCode(normalized)
                .filter(promo -> promo.getUserId().equals(userId))
                .filter(promo -> !Boolean.TRUE.equals(promo.getUsed()))
                .isPresent();
    }

    private String generateUniqueCode() {
        for (int attempt = 0; attempt < MAX_GENERATION_ATTEMPTS; attempt++) {
            String code = "RP-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase(Locale.ROOT);
            if (!promoCodeRepository.existsByCode(code)) {
                return code;
            }
        }
        throw new IllegalBookingStateException("Could not generate unique promo code");
    }

    private String normalize(String promoCode) {
        return promoCode == null || promoCode.isBlank() ? null : promoCode.trim().toUpperCase(Locale.ROOT);
    }

    private void clearAppliedPromo(UUID bookingId) {
        bookingRepository.findById(bookingId).ifPresent(booking -> {
            booking.setHasPromo(false);
            booking.setAppliedPromoCode(null);
            booking.setPromoDiscountPercent(null);
        });
    }
}
