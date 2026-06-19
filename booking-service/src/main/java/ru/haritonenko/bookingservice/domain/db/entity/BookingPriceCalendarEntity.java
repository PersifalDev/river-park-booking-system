package ru.haritonenko.bookingservice.domain.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "booking_price_calendar", uniqueConstraints = {
        @UniqueConstraint(
                name = "uk_booking_price_calendar_category_date_rate_plan",
                columnNames = {"room_category_id", "calendar_date", "rate_plan_id"}
        )
})
public class BookingPriceCalendarEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @Column(name = "room_category_id", nullable = false)
    private Long roomCategoryId;

    @NotNull
    @Column(name = "calendar_date", nullable = false)
    private LocalDate calendarDate;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rate_plan_id", nullable = false)
    private BookingTariffEntity ratePlan;

    @NotNull
    @DecimalMin("0.00")
    @Column(name = "price", nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(name = "available", nullable = false)
    private Boolean available;

    @Column(name = "min_stay")
    private Integer minStay;

    @Column(name = "closed_to_arrival", nullable = false)
    private Boolean closedToArrival;

    @Column(name = "closed_to_departure", nullable = false)
    private Boolean closedToDeparture;

    @Column(name = "season_code", length = 64)
    private String seasonCode;

    @Column(name = "promotion_code", length = 64)
    private String promotionCode;

    @Column(name = "holiday_name", length = 128)
    private String holidayName;

    @Column(name = "occupancy_percent", precision = 5, scale = 2)
    private BigDecimal occupancyPercent;

    @Column(name = "demand_multiplier", precision = 8, scale = 4)
    private BigDecimal demandMultiplier;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    private void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (available == null) {
            available = true;
        }
        if (closedToArrival == null) {
            closedToArrival = false;
        }
        if (closedToDeparture == null) {
            closedToDeparture = false;
        }
        if (demandMultiplier == null) {
            demandMultiplier = BigDecimal.ONE;
        }
    }

    @PreUpdate
    private void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
