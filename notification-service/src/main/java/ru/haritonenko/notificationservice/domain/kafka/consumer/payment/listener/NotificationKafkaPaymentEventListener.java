package ru.haritonenko.notificationservice.domain.kafka.consumer.payment.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.haritonenko.commonlibs.dto.kafka.event.PaymentEvent;
import ru.haritonenko.notificationservice.domain.service.payment.PaymentEventProcessor;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationKafkaPaymentEventListener {

    private final PaymentEventProcessor eventProcessor;

    @KafkaListener(topics = "${app.kafka.consumer.topics.payment-events}", containerFactory = "paymentKafkaListenerContainerFactory")
    public void listenPaymentEvent(ConsumerRecord<UUID, PaymentEvent<?>> record) {
        PaymentEvent<?> event = record.value();
        if (event == null || event.payload() == null) {
            log.warn("Received empty payment event in notification-service");
            return;
        }

        log.info("Payment event received in notification-service: key={}, eventId={}, eventType={}",
                record.key(), event.eventId(), event.eventType());
        eventProcessor.process(event);
    }
}
