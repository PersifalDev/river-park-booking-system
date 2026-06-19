package ru.haritonenko.bookingservice.kafka.outbox.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import ru.haritonenko.bookingservice.kafka.outbox.db.BookingOutboxEntity;
import ru.haritonenko.bookingservice.kafka.outbox.db.repository.BookingOutboxRepository;
import ru.haritonenko.bookingservice.kafka.outbox.exception.KafkaBookingEventIllegalStateException;
import ru.haritonenko.bookingservice.kafka.outbox.status.OutboxStatus;
import ru.haritonenko.commonlibs.dto.kafka.event.BookingKafkaEvent;
import ru.haritonenko.commonlibs.dto.kafka.event.type.BookingEventType;
import ru.haritonenko.commonlibs.dto.kafka.payload.BookingKafkaPayload;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingOutboxServiceTest {

    private final BookingOutboxRepository repository = mock(BookingOutboxRepository.class);
    private final ObjectMapper objectMapper = mock(ObjectMapper.class);

    private final BookingOutboxService service = new BookingOutboxService(repository, objectMapper);

    @Test
    void shouldSaveEventAsNewOutboxEntity() throws Exception {
        BookingKafkaEvent<BookingKafkaPayload> event = event();
        when(objectMapper.writeValueAsString(event)).thenReturn("{\"eventId\":\"%s\"}".formatted(event.eventId()));

        service.saveEvent(event);

        verify(repository).save(argThat(outbox ->
                outbox.getId().equals(event.eventId())
                        && outbox.getAggregateId().equals(event.payload().bookingId())
                        && outbox.getEventType().equals(event.eventType().name())
                        && outbox.getStatus() == OutboxStatus.NEW
                        && outbox.getAttempts() == 0
                        && outbox.getNextAttemptAt() != null
        ));
    }

    @Test
    void shouldThrowWhenEventCanNotBeSerialized() throws Exception {
        BookingKafkaEvent<BookingKafkaPayload> event = event();
        when(objectMapper.writeValueAsString(any())).thenThrow(new JsonProcessingException("boom") {
        });

        assertThrows(KafkaBookingEventIllegalStateException.class, () -> service.saveEvent(event));
    }

    private BookingKafkaEvent<BookingKafkaPayload> event() {
        UUID bookingId = UUID.randomUUID();
        return BookingKafkaEvent.<BookingKafkaPayload>builder()
                .eventId(UUID.randomUUID())
                .correlationId(bookingId.toString())
                .source("booking-service-test")
                .eventType(BookingEventType.BOOKING_CANCELLED)
                .createdAt(OffsetDateTime.now())
                .payload(BookingKafkaPayload.builder()
                        .bookingId(bookingId)
                        .bookingCode("BK-TEST")
                        .userId(10L)
                        .build())
                .build();
    }
}
