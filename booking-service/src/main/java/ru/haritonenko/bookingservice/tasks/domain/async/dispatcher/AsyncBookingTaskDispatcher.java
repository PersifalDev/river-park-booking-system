package ru.haritonenko.bookingservice.tasks.domain.async.dispatcher;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ru.haritonenko.bookingservice.tasks.domain.async.db.entity.AsyncBookingTaskEntity;
import ru.haritonenko.bookingservice.tasks.domain.async.db.repository.AsyncBookingTaskEntityRepository;
import ru.haritonenko.bookingservice.tasks.domain.async.dispatcher.config.AsyncBookingTaskDispatcherProperties;
import ru.haritonenko.bookingservice.tasks.domain.async.processor.AsyncBookingTaskProcessor;
import ru.haritonenko.bookingservice.tasks.domain.async.status.AsyncBookingTaskStatus;
import ru.haritonenko.bookingservice.tasks.domain.async.status.TaskExecutionStatus;

import java.time.OffsetDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@Service
public class AsyncBookingTaskDispatcher {

    private final ExecutorService taskDispatcherThreadPool;
    private final TransactionTemplate transactionTemplate;
    private final AsyncBookingTaskEntityRepository taskRepository;
    private final AsyncBookingTaskProcessor taskProcessor;
    private final AsyncBookingTaskDispatcherProperties properties;

    public AsyncBookingTaskDispatcher(
            @Qualifier("taskDispatcherThreadPool") ExecutorService taskDispatcherThreadPool,
            TransactionTemplate transactionTemplate,
            AsyncBookingTaskEntityRepository taskRepository,
            AsyncBookingTaskProcessor taskProcessor,
            AsyncBookingTaskDispatcherProperties properties
    ) {
        this.taskDispatcherThreadPool = taskDispatcherThreadPool;
        this.transactionTemplate = transactionTemplate;
        this.taskRepository = taskRepository;
        this.taskProcessor = taskProcessor;
        this.properties = properties;
    }

    public void dispatchTask(AsyncBookingTaskEntity task) {
        log.info("Dispatching booking task: taskId={}, bookingId={}, step={}, currentStatus={}",
                task.getId(),
                task.getBookingId(),
                task.getProcessingStep(),
                task.getStatus()
        );
        AsyncBookingTaskEntity inProgressTask = markInProgress(task);
        CompletableFuture
                .supplyAsync(() -> taskProcessor.processTask(inProgressTask), taskDispatcherThreadPool)
                .thenAccept(result -> handleTaskExecuted(inProgressTask, result))
                .exceptionally(ex -> handleExceptionInTaskHappened(inProgressTask, ex));
    }

    private AsyncBookingTaskEntity markInProgress(AsyncBookingTaskEntity task) {
        return transactionTemplate.execute(status -> {
            AsyncBookingTaskEntity savedTask = taskRepository.save(task.toBuilder()
                    .status(AsyncBookingTaskStatus.IN_PROGRESS)
                    .attempts(task.getAttempts() == null ? 1 : task.getAttempts() + 1)
                    .lastError(null)
                    .build());
            log.info("Task marked as in progress: taskId={}, bookingId={}, attempts={}",
                    savedTask.getId(),
                    savedTask.getBookingId(),
                    savedTask.getAttempts()
            );
            return savedTask;
        });
    }

    private Void handleExceptionInTaskHappened(AsyncBookingTaskEntity task, Throwable ex) {
        log.warn("Task failed with unexpected exception: taskId={}, bookingId={}", task.getId(), task.getBookingId(), ex);
        scheduleTaskRetry(task, ex.getMessage());
        return null;
    }

    private void handleTaskExecuted(AsyncBookingTaskEntity task, TaskExecutionStatus taskExecutionStatus) {
        log.info("Task executed: taskId={}, bookingId={}, executionStatus={}",
                task.getId(),
                task.getBookingId(),
                taskExecutionStatus
        );
        switch (taskExecutionStatus) {
            case SUCCESS -> handleTaskSucceeded(task);
            case RETRYABLE_ERROR -> scheduleTaskRetry(task, "Retryable execution error");
            case NON_RETRYABLE_ERROR -> handleTaskFailed(task, "Non retryable execution error");
        }
    }

    private void handleTaskFailed(AsyncBookingTaskEntity task, String errorMessage) {
        transactionTemplate.executeWithoutResult(status -> {
            taskRepository.save(task.toBuilder()
                    .status(AsyncBookingTaskStatus.FAILED_NON_RETRYABLE)
                    .nextAttemptAt(null)
                    .lastError(errorMessage)
                    .build());
            log.warn("Task marked as failed non-retryable: taskId={}, bookingId={}, error={}",
                    task.getId(),
                    task.getBookingId(),
                    errorMessage
            );
        });
    }

    private void scheduleTaskRetry(AsyncBookingTaskEntity task, String errorMessage) {
        log.info("Scheduling retry for task: taskId={}, bookingId={}", task.getId(), task.getBookingId());
        if (task.getAttempts() != null && task.getAttempts() >= properties.getMaxAttempts()) {
            log.warn("Maximum number of task attempts reached: taskId={}, bookingId={}, attempts={}",
                    task.getId(),
                    task.getBookingId(),
                    task.getAttempts()
            );
            handleTaskFailed(task, errorMessage);
            return;
        }

        OffsetDateTime nextAttemptTime = OffsetDateTime.now().plus(properties.getRetryDelay());
        transactionTemplate.executeWithoutResult(status -> {
            taskRepository.save(task.toBuilder()
                    .status(AsyncBookingTaskStatus.FAILED_RETRYABLE)
                    .nextAttemptAt(nextAttemptTime)
                    .lastError(errorMessage)
                    .build());
            log.info("Task retry scheduled: taskId={}, bookingId={}, nextAttemptAt={}, error={}",
                    task.getId(),
                    task.getBookingId(),
                    nextAttemptTime,
                    errorMessage
            );
        });
    }

    private void handleTaskSucceeded(AsyncBookingTaskEntity task) {
        transactionTemplate.executeWithoutResult(status -> {
            taskRepository.save(task.toBuilder()
                    .status(AsyncBookingTaskStatus.SUCCEEDED)
                    .nextAttemptAt(null)
                    .lastError(null)
                    .build());
            log.info("Task marked as succeeded: taskId={}, bookingId={}", task.getId(), task.getBookingId());
        });
    }
}
