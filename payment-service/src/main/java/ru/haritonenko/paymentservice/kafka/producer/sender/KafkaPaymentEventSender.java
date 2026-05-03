package ru.haritonenko.paymentservice.kafka.producer.sender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.haritonenko.commonlibs.dto.kafka.event.PaymentKafkaEvent;
import ru.haritonenko.commonlibs.dto.kafka.payload.PaymentKafkaPayload;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaPaymentEventSender {

    @Value("${app.kafka.producer.topics.payment-events}")
    private String topic;

    private final KafkaTemplate<UUID, PaymentKafkaEvent<PaymentKafkaPayload>> paymentKafkaTemplate;

    public void sendEvent(PaymentKafkaEvent<PaymentKafkaPayload> event) {
        UUID key = event.payload().paymentId();
        log.info("Sending payment event to Kafka: topic={}, eventId={}, eventType={}, paymentId={}", topic, event.eventId(), event.eventType(), key);
        paymentKafkaTemplate.send(topic, key, event).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send payment event: eventId={}, paymentId={}", event.eventId(), key, ex);
                return;
            }
            log.info("Payment event sent successfully: topic={}, partition={}, offset={}", topic, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
        });
    }
}
