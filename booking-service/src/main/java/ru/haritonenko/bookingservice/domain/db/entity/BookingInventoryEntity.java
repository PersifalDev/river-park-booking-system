package ru.haritonenko.bookingservice.domain.db.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Objects;

@Entity
@Getter
@Setter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "booking_inventory", uniqueConstraints = {
        @UniqueConstraint(name = "uk_booking_inventory_category_date",
                columnNames = {"room_category_id", "booking_date"})
})
public class BookingInventoryEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false, updatable = false)
    private Long id;

    @NotNull(message = "Room category id can not be null")
    @Column(name = "room_category_id", nullable = false)
    private Long roomCategoryId;

    @NotNull(message = "Booking date can not be null")
    @Column(name = "booking_date", nullable = false)
    private LocalDate bookingDate;

    @NotNull(message = "Total units can not be null")
    @Min(value = 0, message = "Total units can not be less than zero")
    @Column(name = "total_units", nullable = false)
    private Integer totalUnits;

    @NotNull(message = "Held units can not be null")
    @Min(value = 0, message = "Held units can not be less than zero")
    @Column(name = "held_units", nullable = false)
    private Integer heldUnits;

    @NotNull(message = "Confirmed units can not be null")
    @Min(value = 0, message = "Confirmed units can not be less than zero")
    @Column(name = "confirmed_units", nullable = false)
    private Integer confirmedUnits;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (heldUnits == null) {
            heldUnits = 0;
        }
        if (confirmedUnits == null) {
            confirmedUnits = 0;
        }
    }

    @PreUpdate
    public void onUpdate() {
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
        Class<?> objectEffectiveClass = o instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass() : o.getClass();
        Class<?> thisEffectiveClass = this instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass() : this.getClass();
        if (thisEffectiveClass != objectEffectiveClass) {
            return false;
        }
        BookingInventoryEntity that = (BookingInventoryEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}
