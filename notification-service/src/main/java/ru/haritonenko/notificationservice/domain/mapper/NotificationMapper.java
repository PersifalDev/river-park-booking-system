package ru.haritonenko.notificationservice.domain.mapper;

import org.springframework.stereotype.Component;
import ru.haritonenko.notificationservice.api.dto.NotificationResponseDto;
import ru.haritonenko.notificationservice.domain.db.entity.NotificationEntity;

@Component
public class NotificationMapper {
    public NotificationResponseDto toDto(NotificationEntity entity) {
        return new NotificationResponseDto(
                entity.getId(),
                entity.getUserId(),
                entity.getBookingId(),
                entity.getPaymentId(),
                entity.getTitle(),
                entity.getMessage(),
                entity.getType(),
                entity.getStatus(),
                entity.isRead(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
