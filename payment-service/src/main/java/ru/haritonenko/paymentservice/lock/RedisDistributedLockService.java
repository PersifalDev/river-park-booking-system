package ru.haritonenko.paymentservice.lock;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import ru.haritonenko.commonlibs.concurrency.LockWatchdogScheduler;

@Service
@Slf4j
public class RedisDistributedLockService {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> redisUnlockScript;
    private final DefaultRedisScript<Long> redisRenewScript;
    private final LockWatchdogScheduler watchdogScheduler;

    @Value("${app.payment.lock.wait-time:3s}")
    private Duration defaultWaitTime;

    @Value("${app.payment.lock.lease-time:30s}")
    private Duration defaultLeaseTime;

    @Value("${app.payment.lock.watchdog-enabled:true}")
    private boolean watchdogEnabled;

    @Value("${app.payment.lock.renew-interval:10s}")
    private Duration renewInterval;

    public RedisDistributedLockService(
            StringRedisTemplate redisTemplate,
            @Qualifier("redisUnlockScript") DefaultRedisScript<Long> redisUnlockScript,
            @Qualifier("redisRenewScript") DefaultRedisScript<Long> redisRenewScript,
            @Qualifier("paymentLockWatchdogScheduler") LockWatchdogScheduler watchdogScheduler
    ) {
        this.redisTemplate = redisTemplate;
        this.redisUnlockScript = redisUnlockScript;
        this.redisRenewScript = redisRenewScript;
        this.watchdogScheduler = watchdogScheduler;
    }

    public <T> T execute(String lockKey, Supplier<T> action) {
        return execute(lockKey, defaultWaitTime, defaultLeaseTime, action);
    }

    public void execute(String lockKey, Runnable action) {
        execute(lockKey, () -> {
            action.run();
            return null;
        });
    }

    public <T> T execute(String lockKey, Duration waitTime, Duration leaseTime, Supplier<T> action) {
        String key = "payment-service:lock:" + lockKey;
        String token = UUID.randomUUID().toString();
        long deadline = System.nanoTime() + waitTime.toNanos();

        while (System.nanoTime() <= deadline) {
            if (acquire(key, token, leaseTime)) {
                AtomicBoolean leaseLost = new AtomicBoolean(false);
                ScheduledFuture<?> watchdog = startWatchdog(key, token, leaseTime, leaseLost);
                try {
                    T result = action.get();
                    if (leaseLost.get()) {
                        throw new IllegalStateException("Distributed lock lease was lost: " + lockKey);
                    }
                    return result;
                } finally {
                    if (watchdog != null) {
                        watchdog.cancel(false);
                    }
                    release(key, token);
                }
            }
            sleepBeforeRetry();
        }

        throw new IllegalStateException("Could not acquire distributed lock: " + lockKey);
    }

    private ScheduledFuture<?> startWatchdog(
            String key,
            String token,
            Duration leaseTime,
            AtomicBoolean leaseLost
    ) {
        if (!watchdogEnabled) {
            return null;
        }
        long leaseMillis = leaseTime.toMillis();
        long configuredInterval = renewInterval == null ? leaseMillis / 3 : renewInterval.toMillis();
        long intervalMillis = Math.max(1, Math.min(configuredInterval, Math.max(1, leaseMillis / 2)));
        return watchdogScheduler.scheduleAtFixedRate(
                () -> renewLease(key, token, leaseMillis, leaseLost),
                intervalMillis
        );
    }

    private void renewLease(String key, String token, long leaseMillis, AtomicBoolean leaseLost) {
        try {
            Long renewed = redisTemplate.execute(
                    redisRenewScript,
                    List.of(key),
                    token,
                    Long.toString(leaseMillis)
            );
            if (!Long.valueOf(1L).equals(renewed)) {
                leaseLost.set(true);
                log.error("Distributed lock lease could not be renewed because ownership was lost: key={}", key);
            }
        } catch (RuntimeException exception) {
            leaseLost.set(true);
            log.error("Distributed lock lease renewal failed: key={}", key, exception);
        }
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
            Thread.sleep(50);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for distributed lock", exception);
        }
    }
}
