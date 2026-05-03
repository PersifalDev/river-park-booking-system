package ru.haritonenko.catalogservice.services.domain.db.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.haritonenko.catalogservice.services.domain.db.entity.ServiceItemEntity;

import java.util.List;
import java.util.Optional;
import ru.haritonenko.catalogservice.services.domain.type.ServiceItemType;

@Repository
public interface ServiceItemRepository extends JpaRepository<ServiceItemEntity, Long> {

    List<ServiceItemEntity> findByIsActiveTrueOrderBySortOrderAsc(Pageable pageable);

    Optional<ServiceItemEntity> findByIdAndIsActiveTrue(Long id);

    Optional<ServiceItemEntity> findByTypeAndIsActiveTrue(ServiceItemType type);
}