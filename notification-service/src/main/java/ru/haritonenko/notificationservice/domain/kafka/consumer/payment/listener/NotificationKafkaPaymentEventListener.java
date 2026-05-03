package ru.haritonenko.notificationservice.domain.kafka.consumer.payment.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.haritonenko.commonlibs.dto.kafka.event.PaymentKafkaEvent;
import ru.haritonenko.commonlibs.dto.kafka.payload.PaymentKafkaPayload;
import ru.haritonenko.notificationservice.domain.service.payment.PaymentNotificationService;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationKafkaPaymentEventListener {

    private final PaymentNotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.kafka.consumer.topics.payment-events}", containerFactory = "paymentKafkaListenerContainerFactory")
    public void listenPaymentEvent(ConsumerRecord<UUID, PaymentKafkaEvent<?>> record) {
        PaymentKafkaEvent<?> event = record.value();
        if (event == null || event.payload() == null) {
            log.warn("Received empty payment event in notification-service");
            return;
        }

        PaymentKafkaPayload payload = objectMapper.convertValue(event.payload(), PaymentKafkaPayload.class);
        log.info("Payment event received in notification-service: key={}, eventId={}, eventType={}, payload={}", record.key(), event.eventId(), event.eventType(), payload);
        switch (event.eventType()) {
            case PAYMENT_INVOICE_CREATED -> notificationService.sendPaymentCreatedNotification(payload.bookingId(), payload.paymentId(), payload.userId(), payload.priceAmount());
            case PAYMENT_PENDING -> notificationService.sendPaymentPendingNotification(payload.bookingId(), payload.paymentId(), payload.userId(), payload.priceAmount());
            case PAYMENT_CONFIRMED -> notificationService.sendPaymentConfirmedNotification(payload.bookingId(), payload.paymentId(), payload.userId());
            case PAYMENT_CANCELLED -> notificationService.sendPaymentCancelledNotification(payload.bookingId(), payload.paymentId(), payload.userId(), payload.cancellationReason());
            case PAYMENT_FAILED -> notificationService.sendPaymentFailedNotification(payload.bookingId(), payload.paymentId(), payload.userId(), payload.cancellationReason());
        }
    }
}
