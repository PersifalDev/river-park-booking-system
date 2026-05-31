package ru.haritonenko.bookingservice.domain.db.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.time.OffsetDateTime;
import java.util.Objects;
import java.util.UUID;

@Entity
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "promo_code")
public class PromoCodeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @NotBlank(message = "Promo code can not be blank")
    @Column(name = "code", nullable = false, unique = true, length = 64)
    private String code;

    @NotNull(message = "User id can not be null")
    @Column(name = "user_id", nullable = false)
    private Long userId;

    @NotNull(message = "Source booking id can not be null")
    @Column(name = "source_booking_id", nullable = false)
    private UUID sourceBookingId;

    @Column(name = "redeemed_booking_id")
    private UUID redeemedBookingId;

    @NotNull(message = "Discount percent can not be null")
    @Min(value = 1, message = "Discount percent must be positive")
    @Max(value = 99, message = "Discount percent must be less than 100")
    @Column(name = "discount_percent", nullable = false)
    private Integer discountPercent;

    @NotNull(message = "Used flag can not be null")
    @Column(name = "used", nullable = false)
    private Boolean used;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "redeemed_at")
    private OffsetDateTime redeemedAt;

    @PrePersist
    public void onCreate() {
        createdAt = OffsetDateTime.now();
        if (used == null) {
            used = false;
        }
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
        PromoCodeEntity that = (PromoCodeEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy proxy
                ? proxy.getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }
}
