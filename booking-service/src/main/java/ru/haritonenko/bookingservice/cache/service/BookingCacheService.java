package ru.haritonenko.bookingservice.cache.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class BookingCacheService {

    private static final String BOOKING_PAGES_CACHE = "bookingPages";
    private static final String BOOKING_SEARCH_PAGES_CACHE = "bookingSearchPages";
    private static final String BOOKING_BY_USER_CACHE = "bookingByUser";

    private final CacheManager cacheManager;
    private final Map<Long, Set<String>> bookingPageKeysByUser = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> bookingSearchPageKeysByUser = new ConcurrentHashMap<>();

    public String registerBookingPageKey(Long userId, String cacheKey) {
        registerKey(bookingPageKeysByUser, userId, cacheKey);
        return cacheKey;
    }

    public String registerBookingSearchPageKey(Long userId, String cacheKey) {
        registerKey(bookingSearchPageKeysByUser, userId, cacheKey);
        return cacheKey;
    }

    public void evictUserPages(Long userId) {
        evictIndexedKeys(BOOKING_PAGES_CACHE, bookingPageKeysByUser.remove(userId));
        evictIndexedKeys(BOOKING_SEARCH_PAGES_CACHE, bookingSearchPageKeysByUser.remove(userId));
    }

    public void evictAll() {
        evictAll(BOOKING_BY_USER_CACHE);
        evictAll(BOOKING_PAGES_CACHE);
        evictAll(BOOKING_SEARCH_PAGES_CACHE);
        bookingPageKeysByUser.clear();
        bookingSearchPageKeysByUser.clear();
    }

    public void evictBookingByUser(Long userId, Object bookingId) {
        Cache cache = cacheManager.getCache(BOOKING_BY_USER_CACHE);
        if (cache != null) {
            cache.evict(userId + ":" + bookingId);
        }
    }

    private void registerKey(Map<Long, Set<String>> keyIndex, Long userId, String cacheKey) {
        keyIndex.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(cacheKey);
    }

    private void evictIndexedKeys(String cacheName, Set<String> keys) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache == null) {
            return;
        }

        if (keys == null || keys.isEmpty()) {
            return;
        }

        keys.forEach(cache::evict);
    }

    private void evictAll(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
