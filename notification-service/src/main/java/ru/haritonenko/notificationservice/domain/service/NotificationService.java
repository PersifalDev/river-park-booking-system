package ru.haritonenko.notificationservice.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.haritonenko.commonlibs.dto.kafka.event.type.NotificationEventType;
import ru.haritonenko.commonlibs.notification.NotificationStatus;
import ru.haritonenko.commonlibs.utils.pages.CommonPageable;
import ru.haritonenko.notificationservice.api.dto.filter.NotificationPageFilter;
import ru.haritonenko.notificationservice.domain.db.entity.NotificationEntity;
import ru.haritonenko.notificationservice.domain.db.repository.NotificationEntityRepository;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationEntityRepository notificationRepository;

    @Value("${app.notification.default-page-number}")
    private int defaultPageNumber;

    @Value("${app.notification.default-page-size}")
    private int defaultPageSize;

    @Transactional
    public NotificationEntity createNotification(Long userId, UUID bookingId, UUID paymentId, String title, String message, NotificationEventType type) {
        log.info("Creating notification: userId={}, bookingId={}, paymentId={}, type={}", userId, bookingId, paymentId, type);
        NotificationEntity entity = notificationRepository.save(NotificationEntity.builder()
                .userId(userId)
                .bookingId(bookingId)
                .paymentId(paymentId)
                .title(title)
                .message(message)
                .type(type)
                .status(NotificationStatus.NEW)
                .read(false)
                .build());
        log.info("Notification created successfully: notificationId={}, userId={}", entity.getId(), entity.getUserId());
        return entity;
    }

    @Transactional(readOnly = true)
    public Page<NotificationEntity> getAllNotificationsByUserId(Long userId, NotificationPageFilter pageFilter) {
        log.info("Getting all notifications by userId={}", userId);
        Pageable pageable = CommonPageable.getPageable(pageFilter, defaultPageNumber, defaultPageSize);
        return notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<NotificationEntity> getUnreadNotificationsByUserId(Long userId, NotificationPageFilter pageFilter) {
        log.info("Getting unread notifications by userId={}", userId);
        Pageable pageable = CommonPageable.getPageable(pageFilter, defaultPageNumber, defaultPageSize);
        return notificationRepository.findAllByUserIdAndReadFalseOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional
    public NotificationEntity markAsRead(UUID notificationId, Long userId) {
        log.info("Marking notification as read: notificationId={}, userId={}", notificationId, userId);
        NotificationEntity entity = notificationRepository.findById(notificationId).orElseThrow(() -> new IllegalArgumentException("Notification not found id=%s".formatted(notificationId)));
        if (!entity.getUserId().equals(userId)) {
            log.warn("Notification does not belong to user: notificationId={}, requestedUserId={}, ownerUserId={}", notificationId, userId, entity.getUserId());
            throw new IllegalArgumentException("Notification not found id=%s".formatted(notificationId));
        }
        entity.setRead(true);
        entity.setStatus(NotificationStatus.READ);
        NotificationEntity savedEntity = notificationRepository.save(entity);
        log.info("Notification marked as read successfully: notificationId={}, userId={}", savedEntity.getId(), userId);
        return savedEntity;
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        log.info("Marking all notifications as read for userId={}", userId);
        notificationRepository.findAllByUserIdAndReadFalseOrderByCreatedAtDesc(userId, Pageable.unpaged()).forEach(entity -> {
            entity.setRead(true);
            entity.setStatus(NotificationStatus.READ);
            notificationRepository.save(entity);
        });
        log.info("All unread notifications were marked as read for userId={}", userId);
    }
}
