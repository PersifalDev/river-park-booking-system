package ru.haritonenko.notificationservice.domain.kafka.consumer.direct.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.haritonenko.commonlibs.dto.kafka.event.NotificationKafkaEvent;
import ru.haritonenko.commonlibs.dto.kafka.payload.NotificationKafkaPayload;
import ru.haritonenko.notificationservice.domain.service.NotificationService;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationKafkaDirectEventListener {

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.kafka.consumer.topics.notification-events}", containerFactory = "directNotificationKafkaListenerContainerFactory")
    public void listenNotificationEvent(ConsumerRecord<UUID, NotificationKafkaEvent<?>> record) {
        NotificationKafkaEvent<?> event = record.value();
        if (event == null || event.payload() == null) {
            log.warn("Received empty direct notification event in notification-service");
            return;
        }

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
