package ru.haritonenko.userservice.consent.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.haritonenko.userservice.domain.db.entity.UserEntity;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_consents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserConsentEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(name = "consent_type", nullable = false, length = 64)
    private String consentType;

    @Column(name = "version", nullable = false, length = 64)
    private String version;

    @Column(name = "accepted_at", nullable = false)
    private OffsetDateTime acceptedAt;

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (acceptedAt == null) {
            acceptedAt = OffsetDateTime.now();
        }
    }
}
