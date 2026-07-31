package ru.haritonenko.commonlibs.concurrency;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public final class LockWatchdogScheduler implements AutoCloseable {

    private final ScheduledExecutorService executor;

    public LockWatchdogScheduler(String threadNamePrefix) {
        this.executor = Executors.newSingleThreadScheduledExecutor(
                Thread.ofPlatform().name(threadNamePrefix, 0).factory()
        );
    }

    public ScheduledFuture<?> scheduleAtFixedRate(Runnable action, long intervalMillis) {
        return executor.scheduleAtFixedRate(
                action,
                intervalMillis,
                intervalMillis,
                TimeUnit.MILLISECONDS
        );
    }

    @Override
    public void close() {
        executor.shutdown();
    }
}
