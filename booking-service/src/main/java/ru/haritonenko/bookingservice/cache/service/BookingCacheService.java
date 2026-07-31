package ru.haritonenko.bookingservice.cache.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class BookingCacheService {

    private static final String BOOKING_BY_USER_CACHE = "bookingByUser";

    private final CacheManager cacheManager;

    public String bookingByUser(Long userId, UUID bookingId) {
        return userId + ":" + bookingId;
    }

    public String registerBookingByUserKey(Long userId, String cacheKey) {
        return cacheKey;
    }

    public void evictUserPages(Long userId) {
        // Page results are intentionally not cached: they are highly mutable and
        // serializing Spring Data Page implementations to Redis is brittle.
    }

    public void evictUserBookings(Long userId) {
        runAfterCommit(() -> evictAll(BOOKING_BY_USER_CACHE));
    }

    public void evictAll() {
        runAfterCommit(this::evictAllNow);
    }

    public void evictBookingByUser(Long userId, Object bookingId) {
        String cacheKey = userId + ":" + bookingId;
        runAfterCommit(() -> evictBookingByUserNow(userId, cacheKey));
    }

    private void evictAllNow() {
        evictAll(BOOKING_BY_USER_CACHE);
    }

    private void evictBookingByUserNow(Long userId, String cacheKey) {
        Cache cache = cacheManager.getCache(BOOKING_BY_USER_CACHE);
        if (cache != null) {
            cache.evict(cacheKey);
        }
    }

    private void runAfterCommit(Runnable cacheEviction) {
        if (TransactionSynchronizationManager.isActualTransactionActive()
                && TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    evictSafely(cacheEviction);
                }
            });
            return;
        }

        evictSafely(cacheEviction);
    }

    private void evictSafely(Runnable cacheEviction) {
        try {
            cacheEviction.run();
        } catch (RuntimeException exception) {
            log.warn("Cache eviction failed; database state remains authoritative", exception);
        }
    }

    private void evictAll(String cacheName) {
        Cache cache = cacheManager.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        }
    }
}
