package ru.haritonenko.paymentservice.lock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import ru.haritonenko.commonlibs.concurrency.LockWatchdogScheduler;

@Configuration
public class RedisLockConfig {

    @Bean
    public DefaultRedisScript<Long> redisUnlockScript() {
        return new DefaultRedisScript<>(
                "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end",
                Long.class
        );
    }

    @Bean
    public DefaultRedisScript<Long> redisRenewScript() {
        return new DefaultRedisScript<>(
                "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('pexpire', KEYS[1], ARGV[2]) else return 0 end",
                Long.class
        );
    }

    @Bean(destroyMethod = "close")
    public LockWatchdogScheduler paymentLockWatchdogScheduler() {
        return new LockWatchdogScheduler("payment-lock-watchdog-");
    }
}
