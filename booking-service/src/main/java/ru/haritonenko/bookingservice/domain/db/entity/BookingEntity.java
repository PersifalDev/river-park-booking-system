package ru.haritonenko.bookingservice.domain.db.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.proxy.HibernateProxy;
import ru.haritonenko.bookingservice.domain.converter.BookingStatusConverter;
import ru.haritonenko.bookingservice.domain.custom.validation.BookingDateRangeData;
import ru.haritonenko.bookingservice.domain.custom.validation.GuestCountData;
import ru.haritonenko.bookingservice.domain.custom.validation.annotation.ValidBookingDateRange;
import ru.haritonenko.bookingservice.domain.custom.validation.annotation.ValidBookingEntity;
import ru.haritonenko.bookingservice.domain.custom.validation.annotation.ValidGuestCounts;
import ru.haritonenko.bookingservice.domain.status.BookingStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Setter
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@ValidBookingDateRange(
        checkInRequired = true,
        checkOutRequired = true,
        pastAllowed = true
)
@ValidGuestCounts(
        guestsRequired = true,
        adultCountRequired = true,
        childrenCountRequired = true,
        validateComposition = true
)
@ValidBookingEntity
@Table(name = "booking")
@Entity
public class BookingEntity implements BookingDateRangeData, GuestCountData {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @UuidGenerator
    private UUID id;

    @NotNull(message = "User id can not be null")
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @NotNull(message = "Room category id can not be null")
    @Column(name = "room_category_id", nullable = false)
    private Long roomCategoryId;

    @NotBlank(message = "Booking code can not be blank")
    @Column(name = "booking_code", nullable = false, unique = true, updatable = false, length = 64)
    private String bookingCode;

    @Column(name = "guests", nullable = false)
    private Integer guests;

    @Column(name = "adult_count", nullable = false)
    private Integer adultCount;

    @Column(name = "children_count", nullable = false)
    private Integer childrenCount;

    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @Column(name = "check_out_date", nullable = false)
    private LocalDate checkOutDate;

    @NotNull(message = "Price amount can not be null")
    @DecimalMin(value = "0.01", message = "Price amount must be greater than 0")
    @Column(name = "price_amount", precision = 12, scale = 2, nullable = false)
    private BigDecimal priceAmount;

    @Column(name = "hold_expires_at")
    private OffsetDateTime holdExpiresAt;

    @Column(name = "hold_reminder_sent_at")
    private OffsetDateTime holdReminderSentAt;

    @Column(name = "check_in_reminder_sent_at")
    private OffsetDateTime checkInReminderSentAt;

    @Column(name = "inventory_released_at")
    private OffsetDateTime inventoryReleasedAt;

    @NotNull(message = "Promo flag can not be null")
    @Column(name = "has_promo", nullable = false)
    private Boolean hasPromo;

    @Column(name = "applied_promo_code", length = 64)
    private String appliedPromoCode;

    @Column(name = "generated_promo_code", length = 64)
    private String generatedPromoCode;

    @Column(name = "promo_discount_percent")
    private Integer promoDiscountPercent;

    @NotNull(message = "Booking status can not be null")
    @Column(name = "status", nullable = false)
    @Convert(converter = BookingStatusConverter.class)
    private BookingStatus status;

    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;

    @Column(name = "created_at", updatable = false, nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Override
    public LocalDate checkInDate() {
        return checkInDate;
    }

    @Override
    public LocalDate checkOutDate() {
        return checkOutDate;
    }

    @Override
    public Integer guests() {
        return guests;
    }

    @Override
    public Integer adultCount() {
        return adultCount;
    }

    @Override
    public Integer childrenCount() {
        return childrenCount;
    }

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

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null) {
            return false;
        }
        Class<?> objectEffectiveClass = o instanceof HibernateProxy proxy
                ? proxy.getHibernateLazyInitializer().getPersistentClass()
                : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy proxy
                ? proxy.getHibernateLazyInitializer().getPersistentClass()
                : this.getClass();
        if (thisEffectiveClass != objectEffectiveClass) {
            return false;
        }
        BookingEntity that = (BookingEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy proxy
                ? proxy.getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }
}
