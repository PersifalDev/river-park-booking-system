package ru.haritonenko.catalogservice.photo.category.domain.db.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;
import ru.haritonenko.catalogservice.category.domain.db.entity.RoomCategoryEntity;
import ru.haritonenko.catalogservice.photo.category.domain.converter.RoomCategoryPhotoTypeConverter;
import ru.haritonenko.catalogservice.photo.category.domain.type.RoomCategoryPhotoType;

import java.time.OffsetDateTime;
import java.util.Objects;

@Setter
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "room_category_photo")
@Entity
public class RoomCategoryPhotoEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Room category can not be null")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_category_id", nullable = false)
    private RoomCategoryEntity roomCategory;

    @NotNull(message = "Sort order can not be null")
    @Min(value = 0, message = "Sort order must be greater than or equal to 0")
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @NotBlank(message = "Path can not be blank")
    @Column(name = "path", nullable = false)
    private String path;

    @NotNull(message = "Photo type can not be null")
    @Column(name = "photo_type", nullable = false)
    @Convert(converter = RoomCategoryPhotoTypeConverter.class)
    private RoomCategoryPhotoType photoType;

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
        RoomCategoryPhotoEntity that = (RoomCategoryPhotoEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}