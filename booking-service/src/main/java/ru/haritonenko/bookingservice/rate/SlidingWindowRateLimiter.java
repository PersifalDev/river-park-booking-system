package ru.haritonenko.bookingservice.rate;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class SlidingWindowRateLimiter {

    private final ConcurrentMap<String, Deque<Instant>> buckets = new ConcurrentHashMap<>();
    private final Clock clock;

    public SlidingWindowRateLimiter() {
        this(Clock.systemUTC());
    }

    SlidingWindowRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public boolean tryAcquire(String key, Duration window, int maxRequests) {
        Instant now = clock.instant();
        Instant threshold = now.minus(window);
        Deque<Instant> bucket = buckets.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (bucket) {
            while (!bucket.isEmpty() && bucket.peekFirst().isBefore(threshold)) {
                bucket.removeFirst();
            }
            if (bucket.size() >= maxRequests) {
                return false;
            }
            bucket.addLast(now);
            return true;
        }
    }
}
