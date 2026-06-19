package ru.haritonenko.bookingservice.kafka.outbox.dispatcher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.common.KafkaException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import ru.haritonenko.bookingservice.kafka.outbox.db.BookingOutboxEntity;
import ru.haritonenko.bookingservice.kafka.outbox.db.repository.BookingOutboxRepository;
import ru.haritonenko.bookingservice.kafka.outbox.exception.KafkaEventNotFoundException;
import ru.haritonenko.bookingservice.kafka.outbox.status.OutboxStatus;
import ru.haritonenko.bookingservice.kafka.producer.booking.sender.KafkaBookingEventSender;
import ru.haritonenko.commonlibs.dto.kafka.event.BookingKafkaEvent;
import ru.haritonenko.commonlibs.dto.kafka.event.type.BookingEventType;
import ru.haritonenko.commonlibs.dto.kafka.payload.BookingKafkaPayload;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class BookingOutboxDispatcherTest {

    private final BookingOutboxRepository repository = mock(BookingOutboxRepository.class);
    private final KafkaBookingEventSender sender = mock(KafkaBookingEventSender.class);
    private final ObjectMapper objectMapper = mock(ObjectMapper.class);
    private final TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);

    private final BookingOutboxDispatcher dispatcher =
            new BookingOutboxDispatcher(repository, sender, objectMapper, transactionTemplate);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(dispatcher, "batchSize", 10);
        ReflectionTestUtils.setField(dispatcher, "retryDelay", Duration.ofSeconds(30));
        ReflectionTestUtils.setField(dispatcher, "maxAttempts", 3);
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
        doAnswer(invocation -> {
            Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        when(repository.save(any(BookingOutboxEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void shouldDispatchReadyEvents() {
        BookingOutboxEntity outbox = outbox(0);
        when(repository.findReadyForUpdate(eq(OutboxStatus.NEW), any(), any(Pageable.class)))
                .thenReturn(List.of(outbox));
        when(repository.findById(outbox.getId())).thenReturn(Optional.of(outbox));
        mockRead(outbox, event());

        dispatcher.dispatch();

        verify(sender).sendEvent(any());
        assertEquals(OutboxStatus.SENT, outbox.getStatus());
        assertNotNull(outbox.getSentAt());
    }

    @Test
    void shouldDoNothingWhenNoReadyEvents() {
        when(repository.findReadyForUpdate(eq(OutboxStatus.NEW), any(), any(Pageable.class)))
                .thenReturn(List.of());

        dispatcher.dispatch();

        verify(sender, never()).sendEvent(any());
    }

    @Test
    void shouldMarkSentAfterSuccessfulSendOne() {
        BookingOutboxEntity outbox = outbox(0);
        when(repository.findById(outbox.getId())).thenReturn(Optional.of(outbox));
        mockRead(outbox, event());

        dispatcher.sendOne(outbox.getId());

        assertEquals(OutboxStatus.SENT, outbox.getStatus());
        assertNotNull(outbox.getSentAt());
    }

    @Test
    void shouldScheduleRetryAfterKafkaFailure() {
        BookingOutboxEntity outbox = outbox(0);
        when(repository.findById(outbox.getId())).thenReturn(Optional.of(outbox));
        mockRead(outbox, event());
        doThrow(new KafkaException("down")).when(sender).sendEvent(any());

        dispatcher.sendOne(outbox.getId());

        assertEquals(OutboxStatus.NEW, outbox.getStatus());
        assertEquals(1, outbox.getAttempts());
        assertNotNull(outbox.getNextAttemptAt());
    }

    @Test
    void shouldMarkFailedWhenMaxAttemptsReached() {
        BookingOutboxEntity outbox = outbox(2);
        when(repository.findById(outbox.getId())).thenReturn(Optional.of(outbox));
        mockRead(outbox, event());
        doThrow(new KafkaException("down")).when(sender).sendEvent(any());

        dispatcher.sendOne(outbox.getId());

        assertEquals(OutboxStatus.FAILED, outbox.getStatus());
        assertEquals(3, outbox.getAttempts());
    }

    @Test
    void shouldMarkFailedWhenPayloadInvalid() throws Exception {
        BookingOutboxEntity outbox = outbox(0);
        when(repository.findById(outbox.getId())).thenReturn(Optional.of(outbox));
        when(objectMapper.readValue(eq(outbox.getPayload()), any(TypeReference.class)))
                .thenThrow(new JsonProcessingException("bad") {
                });

        dispatcher.sendOne(outbox.getId());

        assertEquals(OutboxStatus.FAILED, outbox.getStatus());
        verify(sender, never()).sendEvent(any());
    }

    @Test
    void shouldThrowWhenEventNotFound() {
        UUID eventId = UUID.randomUUID();
        when(repository.findById(eventId)).thenReturn(Optional.empty());

        assertThrows(KafkaEventNotFoundException.class, () -> dispatcher.sendOne(eventId));
    }

    private void mockRead(BookingOutboxEntity outbox, BookingKafkaEvent<BookingKafkaPayload> event) {
        try {
            when(objectMapper.readValue(eq(outbox.getPayload()), any(TypeReference.class))).thenReturn(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private BookingOutboxEntity outbox(int attempts) {
        return BookingOutboxEntity.builder()
                .id(UUID.randomUUID())
                .aggregateId(UUID.randomUUID())
                .eventType(BookingEventType.BOOKING_CANCELLED.name())
                .payload("{}")
                .status(OutboxStatus.NEW)
                .attempts(attempts)
                .nextAttemptAt(OffsetDateTime.now().minusSeconds(1))
                .build();
    }

    private BookingKafkaEvent<BookingKafkaPayload> event() {
        UUID bookingId = UUID.randomUUID();
        return BookingKafkaEvent.<BookingKafkaPayload>builder()
                .eventId(UUID.randomUUID())
                .correlationId(bookingId.toString())
                .source("booking-service-test")
                .eventType(BookingEventType.BOOKING_CANCELLED)
                .createdAt(OffsetDateTime.now())
                .payload(BookingKafkaPayload.builder().bookingId(bookingId).build())
                .build();
    }
}
