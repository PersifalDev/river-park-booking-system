package ru.haritonenko.bookingservice.lock;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import ru.haritonenko.commonlibs.concurrency.LockWatchdogScheduler;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class RedisDistributedLockServiceTest {

    private final StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
    private final ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
    private final DefaultRedisScript<Long> unlockScript = mock(DefaultRedisScript.class);
    private final DefaultRedisScript<Long> renewScript = mock(DefaultRedisScript.class);
    private final LockWatchdogScheduler watchdogScheduler = mock(LockWatchdogScheduler.class);
    private final BookingLockProperties properties = properties();
    private final RedisDistributedLockService service = new RedisDistributedLockService(
            redisTemplate,
            unlockScript,
            renewScript,
            properties,
            watchdogScheduler
    );

    @Test
    void shouldRenewLeaseAndReleaseOnlyOwnedLock() {
        ScheduledFuture<?> scheduledFuture = mock(ScheduledFuture.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(), any(), eq(properties.getLeaseTime()))).thenReturn(true);
        when(redisTemplate.execute(eq(renewScript), any(List.class), any(), any())).thenReturn(1L);
        when(watchdogScheduler.scheduleAtFixedRate(any(), anyLong())).thenAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return scheduledFuture;
        });

        String result = service.execute("booking:1", () -> "ok");

        assertEquals("ok", result);
        verify(redisTemplate).execute(eq(renewScript), any(List.class), any(), eq("30000"));
        verify(redisTemplate).execute(eq(unlockScript), any(List.class), any());
        verify(scheduledFuture).cancel(false);
    }

    @Test
    void shouldFailWhenLeaseOwnershipIsLost() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(any(), any(), eq(properties.getLeaseTime()))).thenReturn(true);
        when(redisTemplate.execute(eq(renewScript), any(List.class), any(), any())).thenReturn(0L);
        when(watchdogScheduler.scheduleAtFixedRate(any(), anyLong())).thenAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return mock(ScheduledFuture.class);
        });

        assertThrows(IllegalStateException.class, () -> service.execute("booking:1", () -> "ok"));
    }

    private BookingLockProperties properties() {
        BookingLockProperties value = new BookingLockProperties();
        value.setEnabled(true);
        value.setWaitTime(Duration.ofMillis(50));
        value.setLeaseTime(Duration.ofSeconds(30));
        value.setRetrySleepTime(Duration.ofMillis(1));
        value.setWatchdogEnabled(true);
        value.setRenewInterval(Duration.ofSeconds(10));
        return value;
    }
}
