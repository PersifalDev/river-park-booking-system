package ru.haritonenko.notificationservice.domain.db.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.haritonenko.notificationservice.domain.db.entity.NotificationEntity;

import java.util.UUID;

@Repository
public interface NotificationEntityRepository extends JpaRepository<NotificationEntity, UUID> {
    Page<NotificationEntity> findAllByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<NotificationEntity> findAllByUserIdAndReadFalseOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
