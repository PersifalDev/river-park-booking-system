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
import ru.haritonenko.commonlibs.dto.kafka.event.BookingKafkaEvent;
import ru.haritonenko.commonlibs.dto.kafka.payload.BookingKafkaPayload;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingOutboxService {

    private final BookingOutboxRepository repository;
    private final ObjectMapper objectMapper;

    public void saveEvent(BookingKafkaEvent<BookingKafkaPayload> event) {
        try {
            repository.save(BookingOutboxEntity.builder()
                    .id(event.eventId())
                    .aggregateId(event.payload().bookingId())
                    .eventType(event.eventType().name())
                    .payload(objectMapper.writeValueAsString(event))
                    .status(OutboxStatus.NEW)
                    .attempts(0)
                    .nextAttemptAt(OffsetDateTime.now())
                    .build());
            log.info("Event with id={} was successfully saved",event.eventId());
        } catch (JsonProcessingException e) {
            log.warn("Event with id={} wasn't saved",event.eventId(),e);
            throw new KafkaBookingEventIllegalStateException("Can not serialize booking outbox event");
        }
    }
}