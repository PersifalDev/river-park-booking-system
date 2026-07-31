package ru.haritonenko.paymentservice.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ru.haritonenko.commonlibs.dto.kafka.event.PaymentKafkaEvent;
import ru.haritonenko.commonlibs.dto.kafka.payload.PaymentKafkaPayload;
import ru.haritonenko.paymentservice.config.PaymentOutboxProperties;
import ru.haritonenko.paymentservice.kafka.producer.sender.KafkaPaymentEventSender;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletionException;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.payment.outbox",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class PaymentOutboxDispatcher {

    private final PaymentOutboxRepository repository;
    private final KafkaPaymentEventSender sender;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final PaymentOutboxProperties properties;

    @Scheduled(fixedDelayString = "${app.payment.outbox.poll-delay-ms:5000}")
    public void dispatch() {
        OffsetDateTime now = OffsetDateTime.now();
        List<PaymentOutboxEntity> events = transactionTemplate.execute(status -> {
            List<PaymentOutboxEntity> claimed = repository.findReadyForUpdate(
                    now,
                    properties.getBatchSize()
            );
            if (claimed == null || claimed.isEmpty()) {
                return claimed;
            }
            OffsetDateTime leaseUntil = now.plus(properties.getProcessingTimeout());
            claimed.forEach(event -> {
                event.setStatus(PaymentOutboxStatus.PROCESSING);
                event.setNextAttemptAt(leaseUntil);
            });
            return repository.saveAll(claimed);
        });

        if (events == null) {
            return;
        }
        events.forEach(event -> sendOne(event.getId()));
    }

    public void sendOne(UUID eventId) {
        PaymentOutboxEntity event = find(eventId);
        try {
            PaymentKafkaEvent<PaymentKafkaPayload> kafkaEvent = objectMapper.readValue(
                    event.getPayload(),
                    new TypeReference<PaymentKafkaEvent<PaymentKafkaPayload>>() {
                    }
            );
            sender.sendEvent(kafkaEvent).join();
        } catch (JsonProcessingException exception) {
            markFailed(eventId, "Payload deserialization failed");
            return;
        } catch (RuntimeException exception) {
            log.warn("Payment outbox send failed: eventId={}", eventId, exception);
            scheduleRetryOrFail(eventId, exception.getMessage());
            return;
        }
        markSent(eventId);
    }

    private void markSent(UUID eventId) {
        transactionTemplate.executeWithoutResult(status -> {
            PaymentOutboxEntity event = find(eventId);
            event.setStatus(PaymentOutboxStatus.SENT);
            event.setSentAt(OffsetDateTime.now());
            repository.save(event);
        });
    }

    private void scheduleRetryOrFail(UUID eventId, String error) {
        transactionTemplate.executeWithoutResult(status -> {
            PaymentOutboxEntity event = find(eventId);
            int attempts = event.getAttempts() + 1;
            event.setAttempts(attempts);
            if (attempts >= properties.getMaxAttempts()) {
                event.setStatus(PaymentOutboxStatus.FAILED);
            } else {
                event.setStatus(PaymentOutboxStatus.NEW);
                event.setNextAttemptAt(OffsetDateTime.now().plus(properties.getRetryDelay()));
            }
            repository.save(event);
            log.warn("Payment outbox retry result: eventId={}, status={}, attempts={}, error={}",
                    eventId, event.getStatus(), attempts, error);
        });
    }

    private void markFailed(UUID eventId, String error) {
        transactionTemplate.executeWithoutResult(status -> {
            PaymentOutboxEntity event = find(eventId);
            event.setStatus(PaymentOutboxStatus.FAILED);
            repository.save(event);
            log.warn("Payment outbox event failed permanently: eventId={}, error={}", eventId, error);
        });
    }

    private PaymentOutboxEntity find(UUID eventId) {
        return repository.findById(eventId)
                .orElseThrow(() -> new IllegalStateException("Payment outbox event not found: " + eventId));
    }
}
