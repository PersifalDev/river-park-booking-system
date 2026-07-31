package ru.haritonenko.bookingservice.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.haritonenko.bookingservice.config.workmode.BookingWorkModeProperties;
import ru.haritonenko.bookingservice.external.client.notification.NotificationServiceHttpClient;
import ru.haritonenko.bookingservice.external.client.payment.PaymentServiceHttpClient;
import ru.haritonenko.bookingservice.kafka.outbox.service.BookingOutboxService;
import ru.haritonenko.commonlibs.dto.kafka.event.BookingKafkaEvent;
import ru.haritonenko.commonlibs.dto.kafka.event.NotificationKafkaEvent;
import ru.haritonenko.commonlibs.dto.kafka.payload.BookingKafkaPayload;
import ru.haritonenko.commonlibs.dto.kafka.payload.NotificationKafkaPayload;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingEventDeliveryService {

    private final BookingWorkModeProperties properties;
    private final BookingOutboxService outboxService;
    private final PaymentServiceHttpClient paymentClient;
    private final NotificationServiceHttpClient notificationClient;

    public void publish(BookingKafkaEvent<BookingKafkaPayload> event) {
        log.info("Publishing booking event in {} mode: eventId={}, eventType={}",
                properties.getWorkMode(), event.eventId(), event.eventType());
        switch (properties.getWorkMode()) {
            case ASYNC -> outboxService.saveEvent(event);
            case SYNC -> {
                paymentClient.handleBookingEvent(event);
                notificationClient.handleBookingEvent(event);
            }
        }
    }

    public void publish(NotificationKafkaEvent<NotificationKafkaPayload> event) {
        log.info("Publishing notification event in {} mode: eventId={}, eventType={}",
                properties.getWorkMode(), event.eventId(), event.eventType());
        switch (properties.getWorkMode()) {
            case ASYNC -> outboxService.saveNotificationEvent(event);
            case SYNC -> notificationClient.handleNotificationEvent(event);
        }
    }
}
