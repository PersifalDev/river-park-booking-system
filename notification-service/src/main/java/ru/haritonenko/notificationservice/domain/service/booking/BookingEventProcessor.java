package ru.haritonenko.notificationservice.domain.service.booking;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.haritonenko.commonlibs.dto.kafka.event.BookingEvent;
import ru.haritonenko.commonlibs.dto.kafka.payload.BookingPayload;
import ru.haritonenko.notificationservice.inbox.ProcessedEventService;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingEventProcessor {

    private final BookingNotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final ProcessedEventService processedEventService;

    public void process(BookingEvent<?> event) {
        if (event == null || event.payload() == null) {
            throw new IllegalArgumentException("Booking event and payload are required");
        }
        processedEventService.processOnce(
                event.eventId(),
                "notification-service:booking-events",
                () -> processNewEvent(event)
        );
    }

    private void processNewEvent(BookingEvent<?> event) {
        BookingPayload payload = objectMapper.convertValue(event.payload(), BookingPayload.class);
        log.info("Processing booking event in notification-service: eventId={}, eventType={}, bookingId={}",
                event.eventId(),
                event.eventType(),
                payload.bookingId());

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
