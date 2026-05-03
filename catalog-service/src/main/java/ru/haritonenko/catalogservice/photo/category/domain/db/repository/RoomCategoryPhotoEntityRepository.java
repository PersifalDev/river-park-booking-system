package ru.haritonenko.catalogservice.photo.category.domain.db.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.haritonenko.catalogservice.photo.category.domain.db.entity.RoomCategoryPhotoEntity;

import java.util.List;

@Repository
public interface RoomCategoryPhotoEntityRepository extends JpaRepository<RoomCategoryPhotoEntity, Long> {

    Page<RoomCategoryPhotoEntity> findByRoomCategoryIdOrderByIdAsc(Long roomCategoryId, Pageable pageable);

    List<RoomCategoryPhotoEntity> findByRoomCategory_IdInOrderByRoomCategory_IdAscIdAsc(List<Long> roomCategoryIds);

    List<RoomCategoryPhotoEntity> findByRoomCategoryIdOrderByIdAsc(Long roomCategoryId);
}