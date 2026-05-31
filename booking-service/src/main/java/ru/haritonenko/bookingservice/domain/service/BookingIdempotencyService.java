package ru.haritonenko.bookingservice.domain.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.haritonenko.bookingservice.api.dto.BookingRequestDto;
import ru.haritonenko.bookingservice.domain.db.entity.BookingIdempotencyKeyEntity;
import ru.haritonenko.bookingservice.domain.db.repository.BookingIdempotencyKeyRepository;
import ru.haritonenko.bookingservice.domain.exception.BookingIdempotencyConflictException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class BookingIdempotencyService {

    private static final int MAX_KEY_LENGTH = 128;

    private final BookingIdempotencyKeyRepository repository;
    private final Duration ttl;

    public BookingIdempotencyService(
            BookingIdempotencyKeyRepository repository,
            @Value("${app.booking.idempotency.ttl:24h}") Duration ttl
    ) {
        this.repository = repository;
        this.ttl = ttl;
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

    public void remember(Long userId, String idempotencyKey, String requestHash, UUID bookingId) {
        String normalizedKey = normalize(idempotencyKey);
        if (normalizedKey == null) {
            return;
        }
        repository.save(BookingIdempotencyKeyEntity.builder()
                .userId(userId)
                .idempotencyKey(normalizedKey)
                .requestHash(requestHash)
                .bookingId(bookingId)
                .expiresAt(OffsetDateTime.now().plus(ttl))
                .build());
    }

    public String hash(BookingRequestDto request) {
        String canonical = "%s|%s|%s|%s|%s|%s|%s".formatted(
                request.categoryId(),
                request.checkInDate(),
                request.checkOutDate(),
                request.guests(),
                request.adultCount(),
                request.childrenCount(),
                normalizePromo(request.promoCode())
        );
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private String normalize(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        String normalized = idempotencyKey.trim();
        if (normalized.length() > MAX_KEY_LENGTH) {
            throw new BookingIdempotencyConflictException("Idempotency key is too long");
        }
        return normalized;
    }

    private String normalizePromo(String promoCode) {
        return promoCode == null || promoCode.isBlank() ? "" : promoCode.trim().toUpperCase();
    }
}
