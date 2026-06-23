package ru.haritonenko.paymentservice.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import ru.haritonenko.paymentservice.api.dto.filter.PaymentPageFilter;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PaymentCacheService {

    private static final String PAYMENTS_BY_BOOKING_CACHE = "paymentsByBooking";
    private static final String PAYMENT_PAGES_CACHE = "paymentPages";

    private final CacheManager cacheManager;
    private final Map<Long, Set<String>> pageKeysByUser = new ConcurrentHashMap<>();

    public String paymentByBooking(Long userId, UUID bookingId) {
        return userId + ":" + bookingId;
    }

    public String registerPaymentPageKey(Long userId, PaymentPageFilter pageFilter) {
        String cacheKey = userId
                + ":page=" + (pageFilter == null || pageFilter.getPageNumber() == null ? "default" : pageFilter.getPageNumber())
                + ":size=" + (pageFilter == null || pageFilter.getPageSize() == null ? "default" : pageFilter.getPageSize());
        pageKeysByUser.computeIfAbsent(userId, ignored -> ConcurrentHashMap.newKeySet()).add(cacheKey);
        return cacheKey;
    }

    public void evictPayment(Long userId, UUID bookingId) {
        Cache cache = cacheManager.getCache(PAYMENTS_BY_BOOKING_CACHE);
        if (cache != null) {
            cache.evict(paymentByBooking(userId, bookingId));
        }
    }

    public void evictUserPages(Long userId) {
        Cache cache = cacheManager.getCache(PAYMENT_PAGES_CACHE);
        Set<String> keys = pageKeysByUser.remove(userId);
        if (cache == null || keys == null || keys.isEmpty()) {
            return;
        }
        keys.forEach(cache::evict);
    }
}
