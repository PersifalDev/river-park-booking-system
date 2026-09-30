package ru.haritonenko.notificationservice.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ru.haritonenko.commonlibs.dto.kafka.event.type.NotificationEventType;
import ru.haritonenko.commonlibs.notification.NotificationStatus;
import ru.haritonenko.notificationservice.cache.NotificationCacheService;
import ru.haritonenko.notificationservice.domain.db.entity.NotificationEntity;
import ru.haritonenko.notificationservice.domain.db.repository.NotificationEntityRepository;
import ru.haritonenko.notificationservice.api.dto.filter.NotificationPageFilter;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;

class NotificationServiceTest {

    private NotificationEntityRepository repository;
    private NotificationCacheService cacheService;
    private NotificationService service;

    @BeforeEach
    void setUp() {
        repository = mock(NotificationEntityRepository.class);
        cacheService = mock(NotificationCacheService.class);
        service = new NotificationService(repository, cacheService);
        ReflectionTestUtils.setField(service, "defaultPageNumber", 0);
        ReflectionTestUtils.setField(service, "defaultPageSize", 10);
    }

    @Test
    void shouldScopeBookingFilterToAuthenticatedOwner() {
        var filter = new NotificationPageFilter();
        var bookingId = UUID.randomUUID();
        filter.setBookingId(bookingId);
        var expected = new PageImpl<NotificationEntity>(List.of());
        when(repository.findAllByUserIdAndBookingIdOrderByCreatedAtDesc(eq(10L), eq(bookingId), any()))
                .thenReturn(expected);
        assertEquals(expected, service.getAllNotificationsByUserId(10L, filter));
        verify(repository).findAllByUserIdAndBookingIdOrderByCreatedAtDesc(eq(10L), eq(bookingId), any());
    }

    @Test
    void shouldCreateNewNotification() {
        when(repository.save(any(NotificationEntity.class))).thenAnswer(invocation -> {
            NotificationEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        NotificationEntity actual = service.createNotification(
                10L,
                UUID.randomUUID(),
                null,
                "Бронь создана",
                "Ожидает оплаты",
                NotificationEventType.BOOKING_CREATED
        );

        assertEquals(NotificationStatus.NEW, actual.getStatus());
        assertTrue(!actual.isRead());
        verify(repository).save(any(NotificationEntity.class));
    }

    @Test
    void shouldMarkOwnedNotificationAsRead() {
        NotificationEntity entity = notification(10L, false);
        when(repository.findById(entity.getId())).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);

        NotificationEntity actual = service.markAsRead(entity.getId(), 10L);

        assertTrue(actual.isRead());
        assertEquals(NotificationStatus.READ, actual.getStatus());
    }

    @Test
    void shouldRejectMarkAsReadForAnotherUser() {
        NotificationEntity entity = notification(20L, false);
        when(repository.findById(entity.getId())).thenReturn(Optional.of(entity));

        assertThrows(IllegalArgumentException.class, () -> service.markAsRead(entity.getId(), 10L));
    }

    @Test
    void shouldMarkAllUnreadNotificationsAsRead() {
        NotificationEntity first = notification(10L, false);
        NotificationEntity second = notification(10L, false);
        when(repository.findAllByUserIdAndReadFalseOrderByCreatedAtDesc(10L, Pageable.unpaged()))
                .thenReturn(new PageImpl<>(List.of(first, second)));

        service.markAllAsRead(10L);

        assertTrue(first.isRead());
        assertTrue(second.isRead());
        verify(repository).save(first);
        verify(repository).save(second);
    }

    @Test
    void shouldEvictNotificationPagesOnlyAfterCommit() {
        when(repository.save(any(NotificationEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            service.createNotification(10L, UUID.randomUUID(), null, "Title", "Message", NotificationEventType.PAYMENT_PENDING);
            verifyNoInteractions(cacheService);
            TransactionSynchronizationManager.getSynchronizations().forEach(synchronization -> synchronization.afterCommit());
            verify(cacheService).evictUserPages(10L);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
            TransactionSynchronizationManager.setActualTransactionActive(false);
        }
    }

    private NotificationEntity notification(Long userId, boolean read) {
        return NotificationEntity.builder()
                .id(UUID.randomUUID())
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
