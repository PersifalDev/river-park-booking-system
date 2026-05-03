package ru.haritonenko.bookingservice.kafka.producer.notification.sender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.haritonenko.commonlibs.dto.kafka.event.NotificationKafkaEvent;
import ru.haritonenko.commonlibs.dto.kafka.payload.NotificationKafkaPayload;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaNotificationEventSender {

    @Value("${app.kafka.producer.topics.notification-events}")
    private String topic;

    private final KafkaTemplate<UUID, NotificationKafkaEvent<NotificationKafkaPayload>> kafkaNotificationTemplate;

    public void sendEvent(NotificationKafkaEvent<NotificationKafkaPayload> event) {
        UUID key = event.payload().notificationId();

        log.info("Sending notification event: eventId={}, eventType={}, notificationId={}",
                event.eventId(),
                event.eventType(),
                key);

        kafkaNotificationTemplate.send(topic, key, event)
                .whenComplete((sendResult, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send notification event: eventId={}, notificationId={}",
                                event.eventId(),
                                key,
                                ex);
                        return;
                    }

                    log.info("Notification event sent successfully: topic={}, key={}, partition={}, offset={}",
                            topic,
                            key,
                            sendResult.getRecordMetadata().partition(),
                            sendResult.getRecordMetadata().offset());
                });
    }
}
