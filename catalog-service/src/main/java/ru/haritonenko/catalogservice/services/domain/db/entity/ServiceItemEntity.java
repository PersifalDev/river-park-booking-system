package ru.haritonenko.catalogservice.services.domain.db.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.proxy.HibernateProxy;
import ru.haritonenko.catalogservice.services.domain.converter.ServiceItemTypeConverter;
import ru.haritonenko.catalogservice.services.domain.type.ServiceItemType;

import java.time.OffsetDateTime;
import java.util.Objects;

@Setter
@Getter
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "service_item")
@Entity
public class ServiceItemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Service type can not be null")
    @Column(name = "type", nullable = false)
    @Convert(converter = ServiceItemTypeConverter.class)
    private ServiceItemType type;

    @NotBlank(message = "Service title can not be blank")
    @Column(name = "title", nullable = false)
    private String title;

    @NotBlank(message = "Service description can not be blank")
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    @NotNull(message = "Active flag can not be null")
    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    @NotNull(message = "Sort order can not be null")
    @Min(value = 0, message = "Sort order must be greater than or equal to 0")
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

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
        ServiceItemEntity that = (ServiceItemEntity) o;
        return getId() != null && Objects.equals(getId(), that.getId());
    }

    @Override
    public final int hashCode() {
        return this instanceof HibernateProxy proxy ? proxy.getHibernateLazyInitializer().getPersistentClass().hashCode() : getClass().hashCode();
    }
}