package ru.haritonenko.bookingservice.tasks.domain.async.dispatcher.executor;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConcurrencyLimitedExecutorServiceTest {

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shouldApplySameAdmissionLimitToBothThreadTypes(boolean virtual) throws Exception {
        var executor = new ConcurrencyLimitedExecutorService(virtual
                ? Executors.newVirtualThreadPerTaskExecutor() : Executors.newFixedThreadPool(2), 2, 1);
        var release = new CountDownLatch(1);
        List<Future<?>> tasks = new ArrayList<>();
        try {
            for (int i = 0; i < 3; i++) {
                tasks.add(executor.submit(() -> {
                    try {
                        release.await();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    }
                }));
            }
            await(() -> executor.getActiveTaskCount() == 2 && executor.getWaitingTaskCount() == 1,
                    Duration.ofSeconds(5));
            assertThrows(RejectedExecutionException.class, () -> executor.execute(() -> {}));
            assertEquals(1, executor.getRejectedTaskCount());
            release.countDown();
            for (var task : tasks) {
                task.get(5, TimeUnit.SECONDS);
            }
            await(() -> executor.getActiveTaskCount() == 0 && executor.getWaitingTaskCount() == 0,
                    Duration.ofSeconds(5));
            executor.submit(() -> {}).get(5, TimeUnit.SECONDS);
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void shutdownShouldCancelWaitingFutureAndReleaseAdmission(boolean virtual) throws Exception {
        var executor = new ConcurrencyLimitedExecutorService(virtual
                ? Executors.newVirtualThreadPerTaskExecutor() : Executors.newFixedThreadPool(1), 1, 1);
        var release = new CountDownLatch(1);
        try {
            executor.submit(() -> {
                try {
                    release.await();
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            });
            await(() -> executor.getActiveTaskCount() == 1, Duration.ofSeconds(5));
            var waiting = executor.submit(() -> {});
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS));
            assertTrue(waiting.isCancelled());
            assertEquals(0, executor.getActiveTaskCount());
            assertEquals(0, executor.getWaitingTaskCount());
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void shouldLimitConcurrentVirtualTasks() throws Exception {
        var executor = new ConcurrencyLimitedExecutorService(
                Executors.newVirtualThreadPerTaskExecutor(),
                2
        );
        var releaseTasks = new CountDownLatch(1);
        var completedTasks = new CountDownLatch(8);
        List<Integer> observedConcurrency = new ArrayList<>();

        try {
            for (int i = 0; i < 8; i++) {
                executor.execute(() -> {
                    synchronized (observedConcurrency) {
                        observedConcurrency.add(executor.getActiveTaskCount());
                    }
                    try {
                        releaseTasks.await();
                    } catch (InterruptedException ex) {
                        Thread.currentThread().interrupt();
                    } finally {
                        completedTasks.countDown();
                    }
                });
            }

            await(() -> executor.getActiveTaskCount() == 2
                    && executor.getWaitingTaskCount() == 6, Duration.ofSeconds(5));
            assertEquals(2, executor.getActiveTaskCount());
            assertEquals(6, executor.getWaitingTaskCount());

            releaseTasks.countDown();
            assertTrue(completedTasks.await(5, TimeUnit.SECONDS));
            assertTrue(observedConcurrency.stream().allMatch(value -> value <= 2));
        } finally {
            releaseTasks.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void shouldRejectNonPositiveLimit() {
        var delegate = Executors.newVirtualThreadPerTaskExecutor();
        try {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> new ConcurrencyLimitedExecutorService(delegate, 0)
            );
        } finally {
            delegate.shutdownNow();
        }
    }

    private void await(Condition condition, Duration timeout) throws InterruptedException {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (!condition.satisfied() && System.nanoTime() < deadline) {
            Thread.sleep(10);
        }
        assertTrue(condition.satisfied(), "Condition was not satisfied before timeout");
    }

    @FunctionalInterface
    private interface Condition {
        boolean satisfied();
    }
}
