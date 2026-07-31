package ru.haritonenko.bookingservice.inbox;

import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcessedEventServiceTest {

    private final ProcessedEventRepository repository = mock(ProcessedEventRepository.class);
    private final ProcessedEventService service = new ProcessedEventService(repository);

    @Test
    void shouldRunActionForNewEvent() {
        UUID eventId = UUID.randomUUID();
        AtomicInteger calls = new AtomicInteger();
        when(repository.tryInsert(eq(eventId), eq("consumer"), any())).thenReturn(1);

        assertTrue(service.processOnce(eventId, "consumer", calls::incrementAndGet));
        assertTrue(calls.get() == 1);
    }

    @Test
    void shouldSkipDuplicateEvent() {
        UUID eventId = UUID.randomUUID();
        AtomicInteger calls = new AtomicInteger();
        when(repository.tryInsert(eq(eventId), eq("consumer"), any())).thenReturn(0);

        assertFalse(service.processOnce(eventId, "consumer", calls::incrementAndGet));
        assertTrue(calls.get() == 0);
    }
}
