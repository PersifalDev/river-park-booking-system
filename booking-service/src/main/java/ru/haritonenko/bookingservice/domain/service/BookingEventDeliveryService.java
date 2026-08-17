package ru.haritonenko.bookingservice.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.haritonenko.bookingservice.config.workmode.BookingWorkModeProperties;
import ru.haritonenko.bookingservice.external.client.notification.NotificationServiceHttpClient;
import ru.haritonenko.bookingservice.external.client.payment.PaymentServiceHttpClient;
import ru.haritonenko.bookingservice.kafka.outbox.service.BookingOutboxService;
import ru.haritonenko.commonlibs.communication.WorkMode;
import ru.haritonenko.commonlibs.dto.kafka.event.BookingEvent;
import ru.haritonenko.commonlibs.dto.kafka.event.NotificationEvent;
import ru.haritonenko.commonlibs.dto.kafka.event.type.EventType;
import ru.haritonenko.commonlibs.dto.kafka.payload.BookingPayload;
import ru.haritonenko.commonlibs.dto.kafka.payload.NotificationPayload;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingEventDeliveryService {

    private final BookingWorkModeProperties properties;
    private final BookingOutboxService outboxService;
    private final PaymentServiceHttpClient paymentClient;
    private final NotificationServiceHttpClient notificationClient;

    public void submitForDelivery(BookingEvent<BookingPayload> event) {
        logSubmitNotificationForDelivery(properties.getWorkMode(), event.eventId(), event.eventType());
        switch (properties.getWorkMode()) {
            case ASYNC -> outboxService.saveEvent(event);
            case SYNC -> {
                paymentClient.handleBookingEvent(event);
                notificationClient.handleBookingEvent(event);
            }
        }
    }

    public void submitForDelivery(NotificationEvent<NotificationPayload> event) {
        logSubmitNotificationForDelivery(properties.getWorkMode(), event.eventId(), event.eventType());
        switch (properties.getWorkMode()) {
            case ASYNC -> outboxService.saveNotificationEvent(event);
            case SYNC -> notificationClient.handleNotificationEvent(event);
        }
    }
    private void logSubmitNotificationForDelivery(WorkMode mode, UUID uuid, EventType type) {
        log.info("Submitting notification event for delivery in {} mode: eventId={}, eventType={}",
                mode, uuid, type);
    }
}
