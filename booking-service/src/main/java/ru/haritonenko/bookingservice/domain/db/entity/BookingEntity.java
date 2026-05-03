package ru.haritonenko.bookingservice.domain.db.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
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
import ru.haritonenko.bookingservice.domain.custom.validation.annotation.NotPastDate;
import ru.haritonenko.bookingservice.domain.custom.validation.annotation.ValidBookingDates;
import ru.haritonenko.bookingservice.domain.custom.validation.annotation.ValidBookingEntity;
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
@ValidBookingDates
@ValidBookingEntity
@Table(name = "booking")
@Entity
public class BookingEntity {

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

    @NotNull(message = "Guests count can not be null")
    @Min(value = 1, message = "Guests count must be greater than or equal to 1")
    @Column(name = "guests", nullable = false)
    private Integer guests;

    @NotNull(message = "Adult count can not be null")
    @Min(value = 1, message = "Adult count must be greater than or equal to 1")
    @Column(name = "adult_count", nullable = false)
    private Integer adultCount;

    @NotNull(message = "Children count can not be null")
    @Min(value = 0, message = "Children count must be greater than or equal to 0")
    @Column(name = "children_count", nullable = false)
    private Integer childrenCount;

    @NotNull(message = "Check in date can not be null")
    @NotPastDate(message = "Check in date can not be in the past")
    @Column(name = "check_in_date", nullable = false)
    private LocalDate checkInDate;

    @NotNull(message = "Check out date can not be null")
    @NotPastDate(message = "Check out date can not be in the past")
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

    @NotNull(message = "Promo flag can not be null")
    @Column(name = "has_promo", nullable = false)
    private Boolean hasPromo;

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
