package ru.haritonenko.userservice.security.refresh.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.haritonenko.userservice.domain.db.entity.UserEntity;
import ru.haritonenko.userservice.security.refresh.db.entity.RefreshTokenEntity;
import ru.haritonenko.userservice.security.refresh.db.repository.RefreshTokenRepository;
import ru.haritonenko.userservice.security.refresh.exception.RefreshTokenException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 32;

    private final RefreshTokenRepository refreshTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${jwt.refresh-lifetime}")
    private long refreshLifetimeMs;

    @Transactional
    public IssuedRefreshToken issue(UserEntity user, String userAgent, String ipAddress) {
        return createToken(user, UUID.randomUUID(), userAgent, ipAddress);
    }

    @Transactional
    public RotatedRefreshToken rotate(String refreshToken, String userAgent, String ipAddress) {
        OffsetDateTime now = OffsetDateTime.now();
        RefreshTokenEntity current = refreshTokenRepository.findByTokenHash(hash(refreshToken))
                .orElseThrow(() -> new RefreshTokenException("Refresh token is invalid"));

        if (current.getRevokedAt() != null) {
            refreshTokenRepository.revokeFamily(current.getFamilyId(), now);
            throw new RefreshTokenException("Refresh token was already used");
        }
        if (!current.getExpiresAt().isAfter(now)) {
            current.setRevokedAt(now);
            throw new RefreshTokenException("Refresh token is expired");
        }

        IssuedRefreshToken next = createToken(current.getUser(), current.getFamilyId(), userAgent, ipAddress);
        current.setRevokedAt(now);
        current.setLastUsedAt(now);
        current.setReplacedBy(next.entity());
        return new RotatedRefreshToken(current.getUser(), next.token(), next.expiresAt());
    }

    private IssuedRefreshToken createToken(UserEntity user, UUID familyId, String userAgent, String ipAddress) {
        String token = generateToken();
        OffsetDateTime expiresAt = OffsetDateTime.now().plusNanos(refreshLifetimeMs * 1_000_000);
        RefreshTokenEntity entity = refreshTokenRepository.save(RefreshTokenEntity.builder()
                .user(user)
                .tokenHash(hash(token))
                .familyId(familyId)
                .expiresAt(expiresAt)
                .userAgent(limit(userAgent, 256))
                .ipAddress(limit(ipAddress, 64))
                .build());
        return new IssuedRefreshToken(entity, token, expiresAt);
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    public record IssuedRefreshToken(RefreshTokenEntity entity, String token, OffsetDateTime expiresAt) {
    }

    public record RotatedRefreshToken(UserEntity user, String token, OffsetDateTime expiresAt) {
    }
}
