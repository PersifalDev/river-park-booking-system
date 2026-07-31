package ru.haritonenko.bookingservice.tasks.domain.async.dispatcher.executor;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class ConcurrencyLimitedExecutorService extends AbstractExecutorService {

    private final ExecutorService delegate;
    private final Semaphore permits;
    private final int maxConcurrency;
    private final AtomicInteger activeTaskCount = new AtomicInteger();
    private final AtomicInteger waitingTaskCount = new AtomicInteger();

    public ConcurrencyLimitedExecutorService(ExecutorService delegate, int maxConcurrency) {
        if (maxConcurrency < 1) {
            throw new IllegalArgumentException("maxConcurrency must be positive");
        }
        this.delegate = Objects.requireNonNull(delegate);
        this.maxConcurrency = maxConcurrency;
        this.permits = new Semaphore(maxConcurrency, true);
    }

    @Override
    public void execute(Runnable command) {
        Objects.requireNonNull(command);
        waitingTaskCount.incrementAndGet();
        try {
            delegate.execute(() -> executeWithPermit(command));
        } catch (RejectedExecutionException ex) {
            waitingTaskCount.decrementAndGet();
            throw ex;
        }
    }

    private void executeWithPermit(Runnable command) {
        boolean acquired = false;
        boolean waiting = true;
        try {
            permits.acquire();
            acquired = true;
            waitingTaskCount.decrementAndGet();
            waiting = false;
            activeTaskCount.incrementAndGet();
            command.run();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } finally {
            if (waiting) {
                waitingTaskCount.decrementAndGet();
            }
            if (acquired) {
                activeTaskCount.decrementAndGet();
                permits.release();
            }
        }
    }

    public int getActiveTaskCount() {
        return activeTaskCount.get();
    }

    public int getWaitingTaskCount() {
        return waitingTaskCount.get();
    }

    public int getMaxConcurrency() {
        return maxConcurrency;
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        return delegate.shutdownNow();
    }

    @Override
    public boolean isShutdown() {
        return delegate.isShutdown();
    }

    @Override
    public boolean isTerminated() {
        return delegate.isTerminated();
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return delegate.awaitTermination(timeout, unit);
    }
}
