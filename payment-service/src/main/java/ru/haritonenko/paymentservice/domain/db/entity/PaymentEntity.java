package ru.haritonenko.paymentservice.domain.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import ru.haritonenko.paymentservice.domain.status.PaymentStatus;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payments")
public class PaymentEntity {

    @Id
    @UuidGenerator
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @NotNull
    @Column(name = "booking_id", nullable = false, unique = true, updatable = false)
    private UUID bookingId;

    @NotBlank
    @Column(name = "booking_code", nullable = false, length = 120)
    private String bookingCode;

    @NotNull
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @NotNull
    @Column(name = "price_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal priceAmount;

    @NotNull
    @Column(name = "status", nullable = false, length = 50)
    private PaymentStatus status;

    @NotBlank
    @Column(name = "payment_method", nullable = false, length = 120)
    private String paymentMethod;

    @NotBlank
    @Column(name = "payment_comment", nullable = false, columnDefinition = "TEXT")
    private String paymentComment;

    @NotBlank
    @Column(name = "contact_phone", nullable = false, length = 120)
    private String contactPhone;

    @NotBlank
    @Column(name = "payment_instruction", nullable = false, columnDefinition = "TEXT")
    private String paymentInstruction;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    private void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    private void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
