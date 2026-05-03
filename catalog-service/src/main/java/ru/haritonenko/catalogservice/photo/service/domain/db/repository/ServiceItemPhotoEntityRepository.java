package ru.haritonenko.catalogservice.photo.service.domain.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.haritonenko.catalogservice.photo.service.domain.db.entity.ServiceItemPhotoEntity;

import java.util.Optional;

@Repository
public interface ServiceItemPhotoEntityRepository extends JpaRepository<ServiceItemPhotoEntity, Long> {

    Optional<ServiceItemPhotoEntity> findByServiceItem_Id(Long serviceItemId);
}
