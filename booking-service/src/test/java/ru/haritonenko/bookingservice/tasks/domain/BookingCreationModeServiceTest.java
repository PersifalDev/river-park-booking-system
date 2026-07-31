package ru.haritonenko.bookingservice.tasks.domain;

import org.junit.jupiter.api.Test;
import ru.haritonenko.bookingservice.config.workmode.BookingWorkModeProperties;
import ru.haritonenko.bookingservice.tasks.domain.async.db.entity.AsyncBookingTaskEntity;
import ru.haritonenko.bookingservice.tasks.domain.async.dispatcher.AsyncBookingTaskDispatcher;
import ru.haritonenko.commonlibs.communication.WorkMode;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class BookingCreationModeServiceTest {

    private final BookingWorkModeProperties properties = new BookingWorkModeProperties();
    private final AsyncBookingTaskDispatcher dispatcher = mock(AsyncBookingTaskDispatcher.class);
    private final BookingCreationModeService service = new BookingCreationModeService(properties, dispatcher);
    private final AsyncBookingTaskEntity task = AsyncBookingTaskEntity.builder()
            .id(1L)
            .bookingId(UUID.randomUUID())
            .build();

    @Test
    void shouldExecuteTaskInCallingThreadInSyncMode() {
        properties.setWorkMode(WorkMode.SYNC);

        service.process(task);

        verify(dispatcher).executeSynchronously(task);
        verify(dispatcher, never()).dispatchTask(task);
    }

    @Test
    void shouldDispatchTaskInAsyncMode() {
        properties.setWorkMode(WorkMode.ASYNC);

        service.process(task);

        verify(dispatcher).dispatchTask(task);
        verify(dispatcher, never()).executeSynchronously(task);
    }
}
