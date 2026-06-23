package ru.haritonenko.bookingservice.lock;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class RedisDistributedLockService {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> redisUnlockScript;
    private final BookingLockProperties properties;

    public <T> T execute(String lockKey, Supplier<T> action) {
        if (!properties.isEnabled()) {
            return action.get();
        }
        return execute(lockKey, properties.getWaitTime(), properties.getLeaseTime(), action);
    }

    public void execute(String lockKey, Runnable action) {
        execute(lockKey, () -> {
            action.run();
            return null;
        });
    }

    public <T> T execute(String lockKey, Duration waitTime, Duration leaseTime, Supplier<T> action) {
        String key = "booking-service:lock:" + lockKey;
        String token = UUID.randomUUID().toString();
        long deadline = System.nanoTime() + waitTime.toNanos();

        while (System.nanoTime() <= deadline) {
            if (acquire(key, token, leaseTime)) {
                try {
                    return action.get();
                } finally {
                    release(key, token);
                }
            }
            sleepBeforeRetry();
        }

        throw new IllegalStateException("Could not acquire distributed lock: " + lockKey);
    }

    private boolean acquire(String key, String token, Duration leaseTime) {
        try {
            return Boolean.TRUE.equals(redisTemplate.opsForValue().setIfAbsent(key, token, leaseTime));
        } catch (RedisConnectionFailureException exception) {
            throw new IllegalStateException("Redis is unavailable for distributed lock: " + key, exception);
        }
    }

    private void release(String key, String token) {
        try {
            redisTemplate.execute(redisUnlockScript, List.of(key), token);
        } catch (RedisConnectionFailureException exception) {
            throw new IllegalStateException("Redis is unavailable during distributed lock release: " + key, exception);
        }
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(properties.getRetrySleepTime().toMillis());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for distributed lock", exception);
        }
    }
}
