package ru.haritonenko.bookingservice.tasks.domain.async.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.Cache;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ru.haritonenko.bookingservice.cache.service.BookingCacheService;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class BookingCacheServiceTransactionTest {

    private static final String BOOKING_BY_USER_CACHE = "bookingByUser";

    private final ConcurrentMapCacheManager cacheManager = new ConcurrentMapCacheManager(BOOKING_BY_USER_CACHE);
    private final BookingCacheService service = new BookingCacheService(cacheManager);

    @AfterEach
    void tearDownTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
        TransactionSynchronizationManager.setActualTransactionActive(false);
    }

    @Test
    void shouldEvictImmediatelyWithoutTransaction() {
        Long userId = 10L;
        UUID bookingId = UUID.randomUUID();
        String cacheKey = service.registerBookingByUserKey(userId, service.bookingByUser(userId, bookingId));
        Cache cache = bookingCache();
        cache.put(cacheKey, "CREATED");

        service.evictBookingByUser(userId, bookingId);

        assertNull(cache.get(cacheKey));
    }

    @Test
    void shouldEvictOnlyAfterTransactionCommit() {
        Long userId = 10L;
        UUID bookingId = UUID.randomUUID();
        String cacheKey = service.registerBookingByUserKey(userId, service.bookingByUser(userId, bookingId));
        Cache cache = bookingCache();
        cache.put(cacheKey, "CREATED");
        beginTransactionSynchronization();

        service.evictBookingByUser(userId, bookingId);

        assertEquals("CREATED", cache.get(cacheKey, String.class));
        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
        assertEquals(1, synchronizations.size());

        synchronizations.forEach(TransactionSynchronization::afterCommit);

        assertNull(cache.get(cacheKey));
    }

    @Test
    void shouldKeepCacheEntryAfterTransactionRollback() {
        Long userId = 10L;
        UUID bookingId = UUID.randomUUID();
        String cacheKey = service.registerBookingByUserKey(userId, service.bookingByUser(userId, bookingId));
        Cache cache = bookingCache();
        cache.put(cacheKey, "CREATED");
        beginTransactionSynchronization();

        service.evictBookingByUser(userId, bookingId);
        TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization ->
                        synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK));

        assertEquals("CREATED", cache.get(cacheKey, String.class));
    }

    private Cache bookingCache() {
        Cache cache = cacheManager.getCache(BOOKING_BY_USER_CACHE);
        assertNotNull(cache);
        return cache;
    }

    private void beginTransactionSynchronization() {
        TransactionSynchronizationManager.setActualTransactionActive(true);
        TransactionSynchronizationManager.initSynchronization();
    }
}
