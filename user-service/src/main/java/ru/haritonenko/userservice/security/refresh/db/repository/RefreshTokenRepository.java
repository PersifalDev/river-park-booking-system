package ru.haritonenko.userservice.security.refresh.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.haritonenko.userservice.security.refresh.db.entity.RefreshTokenEntity;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshTokenEntity, UUID> {

    Optional<RefreshTokenEntity> findByTokenHash(String tokenHash);

    @Modifying
    @Query("""
            update RefreshTokenEntity token
            set token.revokedAt = :revokedAt
            where token.familyId = :familyId
              and token.revokedAt is null
            """)
    int revokeFamily(@Param("familyId") UUID familyId, @Param("revokedAt") OffsetDateTime revokedAt);
}
