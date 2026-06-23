package ru.haritonenko.notificationservice.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import ru.haritonenko.notificationservice.api.dto.filter.NotificationPageFilter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class NotificationCacheService {

    private static final String NOTIFICATION_PAGES_CACHE = "notificationPages";
    private static final String UNREAD_NOTIFICATION_PAGES_CACHE = "unreadNotificationPages";

    private final CacheManager cacheManager;
    private final Map<Long, Set<String>> allPageKeysByUser = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> unreadPageKeysByUser = new ConcurrentHashMap<>();

    public String registerAllPageKey(Long userId, NotificationPageFilter pageFilter) {
        String cacheKey = pageKey(userId, pageFilter);
        allPageKeysByUser.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(cacheKey);
        return cacheKey;
    }

    public String registerUnreadPageKey(Long userId, NotificationPageFilter pageFilter) {
        String cacheKey = pageKey(userId, pageFilter);
        unreadPageKeysByUser.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(cacheKey);
        return cacheKey;
    }

    public void evictUserPages(Long userId) {
        evictIndexedKeys(NOTIFICATION_PAGES_CACHE, allPageKeysByUser.remove(userId));
        evictIndexedKeys(UNREAD_NOTIFICATION_PAGES_CACHE, unreadPageKeysByUser.remove(userId));
    }

    private String pageKey(Long userId, NotificationPageFilter pageFilter) {
        return userId
                + ":page=" + (pageFilter == null || pageFilter.getPageNumber() == null ? "default" : pageFilter.getPageNumber())
                + ":size=" + (pageFilter == null || pageFilter.getPageSize() == null ? "default" : pageFilter.getPageSize());
    }

    private void evictIndexedKeys(String cacheName, Set<String> keys) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null || keys == null || keys.isEmpty()) {
            return;
        }
        keys.forEach(cache::evict);
    }
}
