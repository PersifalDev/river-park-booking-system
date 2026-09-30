package ru.haritonenko.bookingservice.tasks.domain.async.dispatcher.executor;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class ConcurrencyLimitedExecutorService extends AbstractExecutorService {

    private final ExecutorService delegate;
    private final Semaphore permits;
    private final Semaphore admission;
    private final int maxConcurrency;
    private final int maxAdmittedTasks;
    private final AtomicLong rejectedTaskCount = new AtomicLong();
    private final AtomicBoolean forcedShutdown = new AtomicBoolean();
    private final AtomicInteger activeTaskCount = new AtomicInteger();
    private final AtomicInteger waitingTaskCount = new AtomicInteger();

    public ConcurrencyLimitedExecutorService(ExecutorService delegate, int maxConcurrency) {
        this(delegate, maxConcurrency, 64);
    }

    public ConcurrencyLimitedExecutorService(ExecutorService delegate, int maxConcurrency, int queueCapacity) {
        if (maxConcurrency < 1) {
            throw new IllegalArgumentException("maxConcurrency must be positive");
        }
        if (queueCapacity < 0) {
            throw new IllegalArgumentException("queueCapacity must not be negative");
        }
        this.delegate = Objects.requireNonNull(delegate);
        this.maxConcurrency = maxConcurrency;
        this.permits = new Semaphore(maxConcurrency, true);
        this.maxAdmittedTasks = Math.addExact(maxConcurrency, queueCapacity);
        this.admission = new Semaphore(maxAdmittedTasks, true);
    }

    @Override
    public void execute(Runnable command) {
        Objects.requireNonNull(command);
        if (!admission.tryAcquire()) {
            rejectedTaskCount.incrementAndGet();
            throw new RejectedExecutionException("External executor admission limit reached");
        }
        waitingTaskCount.incrementAndGet();
        AdmittedTask task = new AdmittedTask(command);
        try {
            delegate.execute(task);
        } catch (RejectedExecutionException ex) {
            rejectedTaskCount.incrementAndGet();
            task.discard();
            throw ex;
        }
    }

    private final class AdmittedTask implements Runnable {
        private final Runnable command;
        private final AtomicBoolean claimed = new AtomicBoolean();

        private AdmittedTask(Runnable command) {
            this.command = command;
        }

        @Override
        public void run() {
            if (claimed.compareAndSet(false, true)) {
                executeWithPermit(command);
            }
        }

        private void discard() {
            if (claimed.compareAndSet(false, true)) {
                waitingTaskCount.decrementAndGet();
                admission.release();
                cancel(command);
            }
        }
    }

    private static void cancel(Runnable command) {
        if (command instanceof Future<?> future) {
            future.cancel(true);
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
            if (forcedShutdown.get()) {
                cancel(command);
            } else {
                command.run();
            }
        } catch (InterruptedException ex) {
            cancel(command);
            Thread.currentThread().interrupt();
        } finally {
            if (waiting) {
                waitingTaskCount.decrementAndGet();
            }
            if (acquired) {
                activeTaskCount.decrementAndGet();
                permits.release();
            }
            admission.release();
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

    public int getMaxAdmittedTasks() {
        return maxAdmittedTasks;
    }

    public long getRejectedTaskCount() {
        return rejectedTaskCount.get();
    }

    @Override
    public void shutdown() {
        delegate.shutdown();
    }

    @Override
    public List<Runnable> shutdownNow() {
        forcedShutdown.set(true);
        return delegate.shutdownNow().stream().map(runnable -> {
            if (runnable instanceof ConcurrencyLimitedExecutorService.AdmittedTask task) {
                task.discard();
                return task.command;
            }
            return runnable;
        }).toList();
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
