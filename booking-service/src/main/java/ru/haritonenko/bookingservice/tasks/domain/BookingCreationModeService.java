package ru.haritonenko.bookingservice.tasks.domain;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.haritonenko.bookingservice.config.workmode.BookingWorkModeProperties;
import ru.haritonenko.bookingservice.tasks.domain.async.db.entity.AsyncBookingTaskEntity;
import ru.haritonenko.bookingservice.tasks.domain.async.dispatcher.AsyncBookingTaskDispatcher;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingCreationModeService {

    private final BookingWorkModeProperties properties;
    private final AsyncBookingTaskDispatcher taskDispatcher;

    public void process(AsyncBookingTaskEntity task) {
        log.info("Processing booking creation in {} mode: bookingId={}, taskId={}",
                properties.getWorkMode(),
                task.getBookingId(),
                task.getId());

        switch (properties.getWorkMode()) {
            case SYNC -> taskDispatcher.executeSynchronously(task);
            case ASYNC -> taskDispatcher.dispatchTask(task);
        }
    }
}
