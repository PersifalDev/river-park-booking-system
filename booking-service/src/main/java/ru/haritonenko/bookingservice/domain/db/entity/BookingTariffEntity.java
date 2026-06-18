package ru.haritonenko.bookingservice.domain.db.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import ru.haritonenko.bookingservice.domain.tariff.TariffCancellationPolicy;
import ru.haritonenko.bookingservice.domain.tariff.TariffPriceModifierType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "booking_tariff")
public class BookingTariffEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(name = "code", nullable = false, unique = true, length = 64)
    private String code;

    @NotBlank
    @Column(name = "title", nullable = false, length = 128)
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "price_modifier_type", nullable = false, length = 32)
    private TariffPriceModifierType priceModifierType;

    @NotNull
    @Column(name = "price_modifier_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal priceModifierValue;

    @Column(name = "min_nights")
    private Integer minNights;

    @Column(name = "max_nights")
    private Integer maxNights;

    @Column(name = "min_adults")
    private Integer minAdults;

    @Column(name = "min_children")
    private Integer minChildren;

    @Column(name = "active_from")
    private LocalDate activeFrom;

    @Column(name = "active_to")
    private LocalDate activeTo;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "cancellation_policy", nullable = false, length = 32)
    private TariffCancellationPolicy cancellationPolicy;

    @Column(name = "free_cancellation_days_before")
    private Integer freeCancellationDaysBefore;

    @Column(name = "included_services", columnDefinition = "TEXT")
    private String includedServices;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Column(name = "active", nullable = false)
    private Boolean active;

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
