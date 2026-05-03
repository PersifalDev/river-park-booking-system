package ru.haritonenko.notificationservice.domain.kafka.consumer.booking.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.haritonenko.commonlibs.dto.kafka.event.BookingKafkaEvent;
import ru.haritonenko.commonlibs.dto.kafka.payload.BookingKafkaPayload;
import ru.haritonenko.notificationservice.domain.service.booking.BookingNotificationService;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationKafkaBookingEventListener {

    private final BookingNotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.kafka.consumer.topics.booking-events}", containerFactory = "bookingNotificationConsumerFactory")
    public void listenBookingEvent(ConsumerRecord<UUID, BookingKafkaEvent<?>> record) {
        BookingKafkaEvent<?> event = record.value();
        if (event == null || event.payload() == null) {
            log.warn("Received empty booking event in notification-service");
            return;
        }

        BookingKafkaPayload payload = objectMapper.convertValue(event.payload(), BookingKafkaPayload.class);
        log.info("Booking event received in notification-service: key={}, eventId={}, eventType={}, payload={}", record.key(), event.eventId(), event.eventType(), payload);
        switch (event.eventType()) {
            case BOOKING_CREATED -> notificationService.sendBookingCreatedNotification(payload);
            case BOOKING_HOLD_CREATED -> notificationService.sendBookingHoldCreatedNotification(payload);
            case BOOKING_CONFIRMED -> notificationService.sendBookingConfirmedNotification(payload);
            case BOOKING_CANCELLED -> notificationService.sendBookingCancelledNotification(payload);
            case BOOKING_EXPIRED -> notificationService.sendBookingExpiredNotification(payload);
            case BOOKING_FAILED -> notificationService.sendBookingFailedNotification(payload);
        }
    }
}
