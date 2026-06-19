package ru.haritonenko.bookingservice.domain.db.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
import org.hibernate.proxy.HibernateProxy;
import ru.haritonenko.bookingservice.domain.room.RoomHousekeepingStatus;
import ru.haritonenko.bookingservice.domain.room.RoomOperationalStatus;

import java.time.OffsetDateTime;
import java.util.Objects;

@Setter
@Getter
@Builder(toBuilder = true)
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "booking_room")
@Entity
public class BookingRoomEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Room category id can not be null")
    @Column(name = "room_category_id", nullable = false)
    private Long roomCategoryId;

    @NotBlank(message = "Room number can not be blank")
    @Column(name = "room_number", nullable = false, unique = true, length = 32)
    private String roomNumber;

    @NotNull(message = "Room floor can not be null")
    @Column(name = "floor", nullable = false)
    private Integer floor;

    @NotNull(message = "Room operational status can not be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private RoomOperationalStatus status;

    @NotNull(message = "Room housekeeping status can not be null")
    @Enumerated(EnumType.STRING)
    @Column(name = "housekeeping_status", nullable = false, length = 32)
    private RoomHousekeepingStatus housekeepingStatus;

    @Column(name = "maintenance_note", columnDefinition = "TEXT")
    private String maintenanceNote;

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
        BookingRoomEntity that = (BookingRoomEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy proxy
                ? proxy.getHibernateLazyInitializer().getPersistentClass().hashCode()
                : getClass().hashCode();
    }
}
