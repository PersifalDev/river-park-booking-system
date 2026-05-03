package ru.haritonenko.catalogservice.category.domain.db.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;
import ru.haritonenko.catalogservice.category.domain.converter.RoomCategoryNameTypeConverter;
import ru.haritonenko.catalogservice.category.domain.type.RoomType;
import ru.haritonenko.catalogservice.photo.category.domain.db.entity.RoomCategoryPhotoEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Objects;

@Setter
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "room_category")
@Entity
public class RoomCategoryEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Room type can not be null")
    @Column(name = "type_name", nullable = false)
    @Convert(converter = RoomCategoryNameTypeConverter.class)
    private RoomType name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Max guests can not be null")
    @Min(value = 1, message = "Max guests must be greater than or equal to 1")
    @Column(name = "max_guests", nullable = false)
    private Integer maxGuests;

    @NotNull(message = "Base price can not be null")
    @DecimalMin(value = "0.01", message = "Base price must be greater than 0")
    @Column(name = "price", precision = 12, scale = 2, nullable = false)
    private BigDecimal basePrice;

    @NotNull(message = "Area square can not be null")
    @DecimalMin(value = "0.01", message = "Area square must be greater than 0")
    @Column(name = "area_square", precision = 12, scale = 2, nullable = false)
    private BigDecimal areaSquare;

    @NotNull(message = "Total units can not be null")
    @Min(value = 0, message = "Total units must be greater than or equal to 0")
    @Column(name = "total_units", nullable = false)
    private Integer totalUnits;

    @OneToMany(
            mappedBy = "roomCategory",
            fetch = FetchType.LAZY,
            cascade = {CascadeType.PERSIST, CascadeType.MERGE},
            orphanRemoval = true
    )
    @OrderBy("id ASC")
    private List<RoomCategoryPhotoEntity> roomCategoryPhotos;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    private void onCreate() {
        var now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    private void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
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
        RoomCategoryEntity that = (RoomCategoryEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}