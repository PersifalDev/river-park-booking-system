package ru.haritonenko.bookingservice.domain.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(
        name = "booking_idempotency_key",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_booking_idempotency_user_key",
                columnNames = {"user_id", "idempotency_key"}
        )
)
@Entity
public class BookingIdempotencyKeyEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "idempotency_key", nullable = false, updatable = false, length = 128)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false, updatable = false, length = 64)
    private String requestHash;

    @Column(name = "booking_id", nullable = false, updatable = false)
    private UUID bookingId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private OffsetDateTime expiresAt;

    @PrePersist
    private void onCreate() {
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
