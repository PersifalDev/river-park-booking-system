package ru.haritonenko.bookingservice.external.circuit;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;

public class SimpleCircuitBreaker {

    private final String serviceName;
    private final int failureThreshold;
    private final Duration openStateDuration;
    private final Clock clock;

    private int failureCount;
    private Instant openedAt;

    public SimpleCircuitBreaker(String serviceName, int failureThreshold, Duration openStateDuration) {
        this(serviceName, failureThreshold, openStateDuration, Clock.systemUTC());
    }

    SimpleCircuitBreaker(String serviceName, int failureThreshold, Duration openStateDuration, Clock clock) {
        this.serviceName = serviceName;
        this.failureThreshold = failureThreshold;
        this.openStateDuration = openStateDuration;
        this.clock = clock;
    }

    public synchronized <T> T execute(Supplier<T> action) {
        if (isOpen()) {
            throw new ExternalCircuitBreakerOpenException(serviceName);
        }
        try {
            T result = action.get();
            failureCount = 0;
            openedAt = null;
            return result;
        } catch (RuntimeException ex) {
            failureCount++;
            if (failureCount >= failureThreshold) {
                openedAt = clock.instant();
            }
            throw ex;
        }
    }

    private boolean isOpen() {
        if (openedAt == null) {
            return false;
        }
        if (openedAt.plus(openStateDuration).isAfter(clock.instant())) {
            return true;
        }
        failureCount = 0;
        openedAt = null;
        return false;
    }
}
