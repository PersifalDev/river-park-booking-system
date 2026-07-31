package ru.haritonenko.bookingservice.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;
import lombok.extern.slf4j.Slf4j;
import ru.haritonenko.bookingservice.api.dto.BookingRequestDto;
import ru.haritonenko.bookingservice.config.idempotency.BookingIdempotencyProperties;
import ru.haritonenko.bookingservice.domain.db.entity.BookingIdempotencyKeyEntity;
import ru.haritonenko.bookingservice.domain.db.repository.BookingIdempotencyKeyRepository;
import ru.haritonenko.bookingservice.domain.exception.BookingIdempotencyConflictException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class BookingIdempotencyService {

    private final BookingIdempotencyKeyRepository repository;
    private final BookingIdempotencyProperties properties;

    public BookingIdempotencyService(
            BookingIdempotencyKeyRepository repository,
            BookingIdempotencyProperties properties
    ) {
        this.repository = repository;
        this.properties = properties;
    }

    public Optional<BookingIdempotencyKeyEntity> findExisting(Long userId, String idempotencyKey, String requestHash) {
        String normalizedKey = normalize(idempotencyKey);
        if (normalizedKey == null) {
            return Optional.empty();
        }
        return repository.findByUserIdAndIdempotencyKeyAndExpiresAtAfter(userId, normalizedKey, OffsetDateTime.now())
                .map(existing -> {
                    if (!existing.getRequestHash().equals(requestHash)) {
                        throw new BookingIdempotencyConflictException("Idempotency key was already used with another request");
                    }
                    return existing;
                });
    }

    @Transactional
    public void remember(Long userId, String idempotencyKey, String requestHash, UUID bookingId) {
        String normalizedKey = normalize(idempotencyKey);
        if (normalizedKey == null) {
            return;
        }
        OffsetDateTime now = OffsetDateTime.now();
        repository.deleteByUserIdAndIdempotencyKeyAndExpiresAtLessThanEqual(userId, normalizedKey, now);
        repository.save(BookingIdempotencyKeyEntity.builder()
                .userId(userId)
                .idempotencyKey(normalizedKey)
                .requestHash(requestHash)
                .bookingId(bookingId)
                .expiresAt(now.plus(properties.getTtl()))
                .build());
    }

    @Scheduled(fixedDelayString = "${app.booking.idempotency.cleanup-delay-ms:3600000}")
    @Transactional
    public void deleteExpiredKeys() {
        long deleted = repository.deleteByExpiresAtLessThanEqual(OffsetDateTime.now());
        if (deleted > 0) {
            log.info("Expired booking idempotency keys deleted: count={}", deleted);
        }
    }

    public String hash(BookingRequestDto request) {
        String canonical = "%s|%s|%s|%s|%s|%s|%s|%s".formatted(
                request.categoryId(),
                request.checkInDate(),
                request.checkOutDate(),
                request.guests(),
                request.adultCount(),
                request.childrenCount(),
                normalizeCode(request.tariffCode()),
                normalizePromo(request.promoCode())
        );
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    public String lockKey(String idempotencyKey, BookingRequestDto request) {
        String normalizedKey = normalize(idempotencyKey);
        if (normalizedKey != null) {
            return normalizedKey;
        }
        return hash(request);
    }

    private String normalize(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        String normalized = idempotencyKey.trim();
        if (normalized.length() > properties.getMaxKeyLength()) {
            throw new BookingIdempotencyConflictException("Idempotency key is too long");
        }
        return normalized;
    }

    private String normalizePromo(String promoCode) {
        return promoCode == null || promoCode.isBlank() ? "" : promoCode.trim().toUpperCase();
    }

    private String normalizeCode(String code) {
        return code == null || code.isBlank() ? "" : code.trim().toUpperCase();
    }
}
