package ru.haritonenko.paymentservice.kafka.producer.sender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import ru.haritonenko.commonlibs.dto.kafka.event.PaymentEvent;
import ru.haritonenko.commonlibs.dto.kafka.payload.PaymentPayload;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaPaymentEventSender {

    @Value("${app.kafka.producer.topics.payment-events}")
    private String topic;

    private final KafkaTemplate<UUID, PaymentEvent<PaymentPayload>> paymentKafkaTemplate;

    public CompletableFuture<SendResult<UUID, PaymentEvent<PaymentPayload>>> sendEvent(
            PaymentEvent<PaymentPayload> event
    ) {
        UUID key = event.payload().paymentId();
        log.info("Sending payment event to Kafka: topic={}, eventId={}, eventType={}, paymentId={}", topic, event.eventId(), event.eventType(), key);
        CompletableFuture<SendResult<UUID, PaymentEvent<PaymentPayload>>> future =
                paymentKafkaTemplate.send(topic, key, event);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send payment event: eventId={}, paymentId={}", event.eventId(), key, ex);
                return;
            }
            log.info("Payment event sent successfully: topic={}, partition={}, offset={}", topic, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
        });
        return future;
    }
}
