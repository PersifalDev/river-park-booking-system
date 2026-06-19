package ru.haritonenko.bookingservice.tasks.domain.async.dispatcher;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import ru.haritonenko.bookingservice.tasks.domain.async.db.entity.AsyncBookingTaskEntity;
import ru.haritonenko.bookingservice.tasks.domain.async.db.repository.AsyncBookingTaskEntityRepository;
import ru.haritonenko.bookingservice.tasks.domain.async.dispatcher.config.AsyncBookingTaskDispatcherProperties;
import ru.haritonenko.bookingservice.tasks.domain.async.processor.AsyncBookingTaskProcessor;
import ru.haritonenko.bookingservice.tasks.domain.async.status.AsyncBookingTaskStatus;
import ru.haritonenko.bookingservice.tasks.domain.async.status.ProcessingStep;
import ru.haritonenko.bookingservice.tasks.domain.async.status.TaskExecutionStatus;

import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AsyncBookingTaskDispatcherTest {

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private final TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
    private final AsyncBookingTaskEntityRepository taskRepository = mock(AsyncBookingTaskEntityRepository.class);
    private final AsyncBookingTaskProcessor taskProcessor = mock(AsyncBookingTaskProcessor.class);
    private final AsyncBookingTaskDispatcherProperties properties = new AsyncBookingTaskDispatcherProperties();

    private AsyncBookingTaskDispatcher dispatcher;

    @BeforeEach
    void setUp() {
        properties.setMaxAttempts(3);
        properties.setRetryDelay(Duration.ofMillis(10));
        dispatcher = new AsyncBookingTaskDispatcher(
                executorService,
                transactionTemplate,
                taskRepository,
                taskProcessor,
                properties
        );
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
        doAnswer(invocation -> {
            Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        when(taskRepository.save(any(AsyncBookingTaskEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        executorService.shutdownNow();
    }

    @Test
    void shouldMarkTaskSucceededWhenProcessorSucceeds() {
        AsyncBookingTaskEntity task = task(0);
        when(taskProcessor.processTask(any(AsyncBookingTaskEntity.class))).thenReturn(TaskExecutionStatus.SUCCESS);

        dispatcher.dispatchTask(task);

        verify(taskRepository, timeout(1000).atLeast(2)).save(any(AsyncBookingTaskEntity.class));
        verify(taskProcessor, timeout(1000)).processTask(any(AsyncBookingTaskEntity.class));
    }

    @Test
    void shouldScheduleRetryWhenProcessorReturnsRetryableError() {
        AsyncBookingTaskEntity task = task(1);
        when(taskProcessor.processTask(any(AsyncBookingTaskEntity.class))).thenReturn(TaskExecutionStatus.RETRYABLE_ERROR);

        dispatcher.dispatchTask(task);

        verify(taskRepository, timeout(1000).atLeastOnce()).save(argThat(saved ->
                saved.getStatus() == AsyncBookingTaskStatus.FAILED_RETRYABLE
                        && saved.getNextAttemptAt() != null
        ));
    }

    @Test
    void shouldMarkFailedWhenMaxAttemptsReached() {
        AsyncBookingTaskEntity task = task(2);
        when(taskProcessor.processTask(any(AsyncBookingTaskEntity.class))).thenReturn(TaskExecutionStatus.RETRYABLE_ERROR);

        dispatcher.dispatchTask(task);

        verify(taskRepository, timeout(1000).atLeastOnce()).save(argThat(saved ->
                saved.getStatus() == AsyncBookingTaskStatus.FAILED_NON_RETRYABLE
        ));
        verify(taskRepository, atLeastOnce()).save(any(AsyncBookingTaskEntity.class));
    }

    private AsyncBookingTaskEntity task(Integer attempts) {
        return AsyncBookingTaskEntity.builder()
                .id(1L)
                .bookingId(UUID.randomUUID())
                .status(AsyncBookingTaskStatus.NEW)
                .processingStep(ProcessingStep.VALIDATE_REQUEST)
                .attempts(attempts)
                .build();
    }
}
