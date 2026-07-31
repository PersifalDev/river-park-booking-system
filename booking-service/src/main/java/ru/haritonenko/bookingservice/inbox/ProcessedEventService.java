package ru.haritonenko.bookingservice.inbox;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessedEventService {

    private final ProcessedEventRepository repository;

    @Transactional
    public boolean processOnce(UUID eventId, String consumerName, Runnable action) {
        if (eventId == null) {
            throw new IllegalArgumentException("eventId is required for idempotent processing");
        }
        if (repository.tryInsert(eventId, consumerName, OffsetDateTime.now()) == 0) {
            log.info("Duplicate event skipped: eventId={}, consumer={}", eventId, consumerName);
            return false;
        }
        action.run();
        return true;
    }
}
