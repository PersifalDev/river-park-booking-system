package ru.haritonenko.notificationservice.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.haritonenko.commonlibs.dto.kafka.event.NotificationEvent;
import ru.haritonenko.commonlibs.dto.kafka.payload.NotificationPayload;
import ru.haritonenko.notificationservice.inbox.ProcessedEventService;

@Service
@RequiredArgsConstructor
public class DirectNotificationEventProcessor {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final ProcessedEventService processedEventService;

    public void process(NotificationEvent<?> event) {
        if (event == null || event.payload() == null) {
            throw new IllegalArgumentException("Notification event and payload are required");
        }
        processedEventService.processOnce(
                event.eventId(),
                "notification-service:direct-notification-events",
                () -> processNewEvent(event)
        );
    }

    private void processNewEvent(NotificationEvent<?> event) {
        NotificationPayload payload = objectMapper.convertValue(event.payload(), NotificationPayload.class);
        notificationService.createNotification(
                payload.userId(),
                payload.bookingId(),
                payload.paymentId(),
                payload.title(),
                payload.message(),
                payload.notificationType()
        );
    }
}
