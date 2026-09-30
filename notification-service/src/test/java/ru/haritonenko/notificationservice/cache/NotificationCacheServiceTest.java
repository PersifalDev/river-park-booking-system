package ru.haritonenko.notificationservice.cache;

import org.junit.jupiter.api.Test;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import ru.haritonenko.notificationservice.api.dto.filter.NotificationPageFilter;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class NotificationCacheServiceTest {
    @Test
    void shouldSeparateBookingsAndEvictAllOwnerPages() {
        var manager = new ConcurrentMapCacheManager("notificationPages", "unreadNotificationPages");
        var service = new NotificationCacheService(manager);
        var first = new NotificationPageFilter();
        var second = new NotificationPageFilter();
        first.setBookingId(UUID.randomUUID());
        second.setBookingId(UUID.randomUUID());
        String firstKey = service.registerAllPageKey(10L, first);
        String secondKey = service.registerAllPageKey(10L, second);
        String unreadKey = service.registerUnreadPageKey(10L, first);
        assertNotEquals(firstKey, secondKey);
        assertNotEquals(firstKey, service.registerAllPageKey(20L, first));
        manager.getCache("notificationPages").put(firstKey, "first");
        manager.getCache("notificationPages").put(secondKey, "second");
        manager.getCache("unreadNotificationPages").put(unreadKey, "unread");
        service.evictUserPages(10L);
        assertNull(manager.getCache("notificationPages").get(firstKey));
        assertNull(manager.getCache("notificationPages").get(secondKey));
        assertNull(manager.getCache("unreadNotificationPages").get(unreadKey));
    }
}
