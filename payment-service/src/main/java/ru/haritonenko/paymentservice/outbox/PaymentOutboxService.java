package ru.haritonenko.paymentservice.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.haritonenko.commonlibs.dto.kafka.event.PaymentKafkaEvent;
import ru.haritonenko.commonlibs.dto.kafka.payload.PaymentKafkaPayload;

import java.time.OffsetDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentOutboxService {

    private final PaymentOutboxRepository repository;
    private final ObjectMapper objectMapper;

    public void save(PaymentKafkaEvent<PaymentKafkaPayload> event) {
        try {
            repository.save(PaymentOutboxEntity.builder()
                    .id(event.eventId())
                    .aggregateId(event.payload().paymentId())
                    .eventType(event.eventType().name())
                    .payload(objectMapper.writeValueAsString(event))
                    .status(PaymentOutboxStatus.NEW)
                    .attempts(0)
                    .nextAttemptAt(OffsetDateTime.now())
                    .build());
            log.info("Payment outbox event saved: eventId={}, eventType={}",
                    event.eventId(), event.eventType());
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize payment outbox event", exception);
        }
    }
}
