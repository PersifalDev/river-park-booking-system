package ru.haritonenko.catalogservice.category.domain.db.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.haritonenko.catalogservice.category.domain.db.entity.RoomCategoryEntity;
import ru.haritonenko.catalogservice.category.domain.type.RoomType;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface RoomCategoryEntityRepository extends JpaRepository<RoomCategoryEntity, Long> {

    Optional<RoomCategoryEntity> findCategoryById(Long id);

    @Query("""
            SELECT r FROM RoomCategoryEntity r
            WHERE(:roomType IS NULL OR r.name = :roomType)
            AND(:guests IS NULL OR r.maxGuests >= :guests) 
            AND(:priceFrom IS NULL OR r.basePrice >= :priceFrom)
            AND(:priceTo IS NULL OR r.basePrice <= :priceTo)       
            AND(:minArea IS NULL OR r.areaSquare>=:minArea)                
            """)
    Page<RoomCategoryEntity> getRoomCategoriesWithFilter(
            @Param("roomType") RoomType roomType,
            @Param("guests") Integer guests,
            @Param("priceFrom") BigDecimal priceFrom,
            @Param("priceTo") BigDecimal priceTo,
            @Param("minArea") BigDecimal minArea,
            Pageable pageable
    );

    @Query("SELECT r FROM RoomCategoryEntity r")
    Page<RoomCategoryEntity> findAllCategories(Pageable pageable);
}