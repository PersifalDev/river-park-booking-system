package ru.haritonenko.bookingservice.kafka.outbox.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.haritonenko.bookingservice.kafka.outbox.db.BookingOutboxEntity;
import ru.haritonenko.bookingservice.kafka.outbox.db.repository.BookingOutboxRepository;
import ru.haritonenko.bookingservice.kafka.outbox.exception.KafkaBookingEventIllegalStateException;
import ru.haritonenko.bookingservice.kafka.outbox.status.OutboxStatus;
import ru.haritonenko.bookingservice.kafka.outbox.status.OutboxEventKind;
import ru.haritonenko.commonlibs.dto.kafka.event.BookingEvent;
import ru.haritonenko.commonlibs.dto.kafka.event.NotificationEvent;
import ru.haritonenko.commonlibs.dto.kafka.payload.BookingPayload;
import ru.haritonenko.commonlibs.dto.kafka.payload.NotificationPayload;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingOutboxService {

    private final BookingOutboxRepository repository;
    private final ObjectMapper objectMapper;

    public void saveEvent(BookingEvent<BookingPayload> event) {
        save(
                event.eventId(),
                event.payload().bookingId(),
                event.eventType().name(),
                OutboxEventKind.BOOKING,
                event
        );
    }

    public void saveNotificationEvent(NotificationEvent<NotificationPayload> event) {
        save(
                event.eventId(),
                event.payload().bookingId() == null
                        ? event.payload().notificationId()
                        : event.payload().bookingId(),
                event.eventType().name(),
                OutboxEventKind.NOTIFICATION,
                event
        );
    }

    private void save(
            UUID eventId,
            UUID aggregateId,
            String eventType,
            OutboxEventKind eventKind,
            Object event
    ) {
        try {
            repository.save(BookingOutboxEntity.builder()
                    .id(eventId)
                    .aggregateId(aggregateId)
                    .eventType(eventType)
                    .eventKind(eventKind)
                    .payload(objectMapper.writeValueAsString(event))
                    .status(OutboxStatus.NEW)
                    .attempts(0)
                    .nextAttemptAt(OffsetDateTime.now())
                    .build());
            log.info("Outbox event saved: eventId={}, eventKind={}, eventType={}",
                    eventId, eventKind, eventType);
        } catch (JsonProcessingException e) {
            log.warn("Outbox event could not be serialized: eventId={}, eventKind={}", eventId, eventKind, e);
            throw new KafkaBookingEventIllegalStateException("Can not serialize outbox event");
        }
    }
}
