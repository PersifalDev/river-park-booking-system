package ru.haritonenko.notificationservice.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.haritonenko.commonlibs.dto.kafka.event.NotificationKafkaEvent;
import ru.haritonenko.commonlibs.dto.kafka.payload.NotificationKafkaPayload;
import ru.haritonenko.notificationservice.inbox.ProcessedEventService;

@Service
@RequiredArgsConstructor
public class DirectNotificationEventProcessor {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;
    private final ProcessedEventService processedEventService;

    public void process(NotificationKafkaEvent<?> event) {
        if (event == null || event.payload() == null) {
            throw new IllegalArgumentException("Notification event and payload are required");
        }
        processedEventService.processOnce(
                event.eventId(),
                "notification-service:direct-notification-events",
                () -> processNewEvent(event)
        );
    }

    private void processNewEvent(NotificationKafkaEvent<?> event) {
        NotificationKafkaPayload payload = objectMapper.convertValue(event.payload(), NotificationKafkaPayload.class);
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
