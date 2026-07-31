package ru.haritonenko.notificationservice.domain.service.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.haritonenko.commonlibs.dto.kafka.event.PaymentKafkaEvent;
import ru.haritonenko.commonlibs.dto.kafka.payload.PaymentKafkaPayload;
import ru.haritonenko.notificationservice.inbox.ProcessedEventService;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventProcessor {

    private final PaymentNotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final ProcessedEventService processedEventService;

    public void process(PaymentKafkaEvent<?> event) {
        if (event == null || event.payload() == null) {
            throw new IllegalArgumentException("Payment event and payload are required");
        }
        processedEventService.processOnce(
                event.eventId(),
                "notification-service:payment-events",
                () -> processNewEvent(event)
        );
    }

    private void processNewEvent(PaymentKafkaEvent<?> event) {
        PaymentKafkaPayload payload = objectMapper.convertValue(event.payload(), PaymentKafkaPayload.class);
        log.info("Processing payment event in notification-service: eventId={}, eventType={}, paymentId={}",
                event.eventId(),
                event.eventType(),
                payload.paymentId());

        switch (event.eventType()) {
            case PAYMENT_INVOICE_CREATED ->
                    notificationService.sendPaymentCreatedNotification(payload.bookingId(), payload.paymentId(), payload.userId(), payload.priceAmount());
            case PAYMENT_PENDING ->
                    notificationService.sendPaymentPendingNotification(payload.bookingId(), payload.paymentId(), payload.userId(), payload.priceAmount());
            case PAYMENT_CONFIRMED ->
                    notificationService.sendPaymentConfirmedNotification(payload.bookingId(), payload.paymentId(), payload.userId());
            case PAYMENT_CANCELLED ->
                    log.info("Skip PAYMENT_CANCELLED notification because booking cancellation notification is sufficient: bookingId={}, paymentId={}",
                            payload.bookingId(), payload.paymentId());
            case PAYMENT_FAILED ->
                    notificationService.sendPaymentFailedNotification(payload.bookingId(), payload.paymentId(), payload.userId(), payload.cancellationReason());
        }
    }
}
