package ru.haritonenko.paymentservice.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.haritonenko.commonlibs.dto.kafka.event.PaymentKafkaEvent;
import ru.haritonenko.commonlibs.dto.kafka.payload.PaymentKafkaPayload;
import ru.haritonenko.paymentservice.config.PaymentWorkModeProperties;
import ru.haritonenko.paymentservice.external.BookingServiceHttpClient;
import ru.haritonenko.paymentservice.external.NotificationServiceHttpClient;
import ru.haritonenko.paymentservice.outbox.PaymentOutboxService;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventDeliveryService {

    private final PaymentWorkModeProperties properties;
    private final PaymentOutboxService outboxService;
    private final BookingServiceHttpClient bookingClient;
    private final NotificationServiceHttpClient notificationClient;

    public void publish(PaymentKafkaEvent<PaymentKafkaPayload> event) {
        log.info("Publishing payment event in {} mode: eventId={}, eventType={}",
                properties.getWorkMode(), event.eventId(), event.eventType());
        switch (properties.getWorkMode()) {
            case ASYNC -> outboxService.save(event);
            case SYNC -> {
                bookingClient.handlePaymentEvent(event);
                notificationClient.handlePaymentEvent(event);
            }
        }
    }
}
