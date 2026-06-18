package ru.haritonenko.catalogservice.domain.db.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.jdbc.Sql;
import ru.haritonenko.catalogservice.category.domain.db.entity.RoomCategoryEntity;
import ru.haritonenko.catalogservice.category.domain.db.repository.RoomCategoryEntityRepository;
import ru.haritonenko.catalogservice.category.domain.type.RoomType;
import ru.haritonenko.catalogservice.services.domain.db.entity.ServiceItemEntity;
import ru.haritonenko.catalogservice.services.domain.db.repository.ServiceItemRepository;
import ru.haritonenko.catalogservice.services.domain.type.ServiceItemType;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest(properties = {
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@Sql(statements = {
        "drop table if exists public.service_item_photo",
        "drop table if exists public.room_category_photo",
        "drop table if exists public.service_item",
        "drop table if exists public.room_category",
        "create table public.room_category (id bigint auto_increment primary key, type_name integer not null, description text, max_guests integer not null, price numeric(12, 2) not null, area_square numeric(12, 2) not null, total_units integer not null, created_at timestamp not null, updated_at timestamp not null)",
        "create table public.service_item (id bigint auto_increment primary key, type integer not null, title varchar(255) not null, description text not null, is_active boolean not null, sort_order integer not null, created_at timestamp not null, updated_at timestamp not null)"
})
class CatalogRepositoryTest {

    @Autowired
    private RoomCategoryEntityRepository roomCategoryRepository;

    @Autowired
    private ServiceItemRepository serviceItemRepository;

    @Test
    void shouldFilterRoomCategoriesByGuestsPriceAndArea() {
        roomCategoryRepository.save(room(RoomType.STANDARD, 2, BigDecimal.valueOf(5000), BigDecimal.valueOf(20), 10));
        roomCategoryRepository.save(room(RoomType.STUDIO, 4, BigDecimal.valueOf(9000), BigDecimal.valueOf(40), 5));
        roomCategoryRepository.flush();

        var result = roomCategoryRepository.getRoomCategoriesWithFilter(
                null,
                3,
                BigDecimal.valueOf(7000),
                BigDecimal.valueOf(10000),
                BigDecimal.valueOf(30),
                PageRequest.of(0, 10)
        );

        assertEquals(1, result.getTotalElements());
        assertEquals(RoomType.STUDIO, result.getContent().getFirst().getName());
    }

    @Test
    void shouldFindOnlyActiveServicesOrderedBySortOrder() {
        serviceItemRepository.save(service(ServiceItemType.GYM, true, 20));
        serviceItemRepository.save(service(ServiceItemType.CHILDREN_PLAYROOM, true, 10));
        serviceItemRepository.save(service(ServiceItemType.SAUNA_HAMMAM_POOL, false, 1));
        serviceItemRepository.flush();

        var result = serviceItemRepository.findByIsActiveTrueOrderBySortOrderAsc(PageRequest.of(0, 10));

        assertEquals(2, result.size());
        assertEquals(ServiceItemType.CHILDREN_PLAYROOM, result.getFirst().getType());
        assertTrue(serviceItemRepository.findByTypeAndIsActiveTrue(ServiceItemType.SAUNA_HAMMAM_POOL).isEmpty());
    }

    private RoomCategoryEntity room(RoomType type, int guests, BigDecimal price, BigDecimal area, int totalUnits) {
        return RoomCategoryEntity.builder()
                .name(type)
                .description(type.getValue())
                .maxGuests(guests)
                .basePrice(price)
                .areaSquare(area)
                .totalUnits(totalUnits)
                .build();
    }

    private ServiceItemEntity service(ServiceItemType type, boolean active, int sortOrder) {
        return ServiceItemEntity.builder()
                .type(type)
                .title(type.getValue())
                .description(type.getValue() + " description")
                .isActive(active)
                .sortOrder(sortOrder)
                .build();
    }
}
