package ru.haritonenko.notificationservice.domain.db.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.PageRequest;
import ru.haritonenko.commonlibs.dto.kafka.event.type.NotificationEventType;
import ru.haritonenko.commonlibs.notification.NotificationStatus;
import ru.haritonenko.notificationservice.domain.db.entity.NotificationEntity;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest(properties = {
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class NotificationEntityRepositoryTest {

    @Autowired
    private NotificationEntityRepository repository;

    @Test
    void shouldFindNotificationsOnlyForUser() {
        repository.save(notification(10L, false));
        repository.save(notification(20L, false));
        repository.flush();

        var result = repository.findAllByUserIdOrderByCreatedAtDesc(10L, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals(10L, result.getContent().getFirst().getUserId());
    }

    @Test
    void shouldFindOnlyUnreadNotificationsForUser() {
        repository.save(notification(10L, false));
        repository.save(notification(10L, true));
        repository.save(notification(20L, false));
        repository.flush();

        var result = repository.findAllByUserIdAndReadFalseOrderByCreatedAtDesc(10L, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        assertEquals(NotificationStatus.NEW, result.getContent().getFirst().getStatus());
    }

    private NotificationEntity notification(Long userId, boolean read) {
        return NotificationEntity.builder()
                .userId(userId)
                .bookingId(UUID.randomUUID())
                .title("Бронь обновлена")
                .message("Статус брони изменился")
                .type(NotificationEventType.BOOKING_CONFIRMED)
                .status(read ? NotificationStatus.READ : NotificationStatus.NEW)
                .read(read)
                .build();
    }
}
