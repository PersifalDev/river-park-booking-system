package ru.haritonenko.bookingservice.kafka.outbox.dispatcher;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.KafkaException;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ru.haritonenko.bookingservice.kafka.outbox.db.BookingOutboxEntity;
import ru.haritonenko.bookingservice.kafka.outbox.db.repository.BookingOutboxRepository;
import ru.haritonenko.bookingservice.kafka.outbox.config.BookingOutboxProperties;
import ru.haritonenko.bookingservice.kafka.outbox.exception.KafkaEventNotFoundException;
import ru.haritonenko.bookingservice.kafka.outbox.status.OutboxStatus;
import ru.haritonenko.bookingservice.kafka.producer.booking.sender.KafkaBookingEventSender;
import ru.haritonenko.commonlibs.dto.kafka.event.BookingKafkaEvent;
import ru.haritonenko.commonlibs.dto.kafka.payload.BookingKafkaPayload;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingOutboxDispatcher {

    private final BookingOutboxRepository repository;
    private final KafkaBookingEventSender sender;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final BookingOutboxProperties properties;

    @Scheduled(fixedDelayString = "${app.booking.outbox.poll-delay-ms}")
    public void dispatch() {
        OffsetDateTime now = OffsetDateTime.now();

        log.debug("Outbox dispatcher started: now={}", now);

        List<BookingOutboxEntity> events = transactionTemplate.execute(status ->
                repository.findReadyForUpdate(
                        OutboxStatus.NEW,
                        now,
                        PageRequest.of(properties.getPageNumber(), properties.getBatchSize())
                )
        );

        if (events == null || events.isEmpty()) {
            log.debug("No ready outbox events found");
            return;
        }

        log.info("Ready outbox events found: count={}", events.size());

        for (BookingOutboxEntity event : events) {
            sendOne(event.getId());
        }
    }

    public void sendOne(UUID eventId) {
        BookingOutboxEntity event = findOutboxEvent(eventId);

        BookingKafkaEvent<BookingKafkaPayload> kafkaEvent;

        try {
            kafkaEvent = objectMapper.readValue(
                    event.getPayload(),
                    new TypeReference<BookingKafkaEvent<BookingKafkaPayload>>() {
                    }
            );
        } catch (JsonProcessingException e) {
            log.warn("Outbox event payload deserialization failed: eventId={}", eventId, e);
            markFailed(eventId, "Payload deserialization failed");
            return;
        }

        try {
            sender.sendEvent(kafkaEvent);
            markSent(eventId);
        } catch (KafkaException | IllegalStateException e) {
            log.warn("Outbox event sending failed: eventId={}", eventId, e);
            scheduleRetryOrFail(eventId);
        }
    }

    private void markSent(UUID eventId) {
        transactionTemplate.executeWithoutResult(status -> {
            BookingOutboxEntity eventToUpdate = findOutboxEvent(eventId);

            eventToUpdate.setStatus(OutboxStatus.SENT);
            eventToUpdate.setSentAt(OffsetDateTime.now());

            repository.save(eventToUpdate);

            log.info("Outbox event status successfully updated: eventId={}, status={}",
                    eventId,
                    eventToUpdate.getStatus()
            );
        });
    }

    private void scheduleRetryOrFail(UUID eventId) {
        transactionTemplate.executeWithoutResult(status -> {
            BookingOutboxEntity eventToUpdate = findOutboxEvent(eventId);

            int attempts = eventToUpdate.getAttempts() + 1;
            eventToUpdate.setAttempts(attempts);

            if (attempts >= properties.getMaxAttempts()) {
                eventToUpdate.setStatus(OutboxStatus.FAILED);

                log.warn("Outbox event marked as FAILED: eventId={}, attempts={}, maxAttempts={}",
                        eventId,
                        attempts,
                        properties.getMaxAttempts()
                );
            } else {
                OffsetDateTime nextAttemptAt = OffsetDateTime.now().plus(properties.getRetryDelay());

                eventToUpdate.setNextAttemptAt(nextAttemptAt);

                log.info("Outbox event retry scheduled: eventId={}, attempts={}, nextAttemptAt={}",
                        eventId,
                        attempts,
                        nextAttemptAt
                );
            }

            repository.save(eventToUpdate);
        });
    }

    private void markFailed(UUID eventId, String reason) {
        transactionTemplate.executeWithoutResult(status -> {
            BookingOutboxEntity eventToUpdate = findOutboxEvent(eventId);

            eventToUpdate.setStatus(OutboxStatus.FAILED);

            repository.save(eventToUpdate);

            log.warn("Outbox event marked as FAILED: eventId={}, reason={}", eventId, reason);
        });
    }

    private BookingOutboxEntity findOutboxEvent(UUID eventId) {
        return repository.findById(eventId)
                .orElseThrow(() -> {
                    log.warn("Event={} not found in db", eventId);
                    return new KafkaEventNotFoundException(
                            "Event not found with id=%s".formatted(eventId)
                    );
                });
    }
}
