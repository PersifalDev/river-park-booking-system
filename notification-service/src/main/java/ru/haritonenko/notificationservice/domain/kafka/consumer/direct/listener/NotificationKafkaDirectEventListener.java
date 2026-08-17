package ru.haritonenko.notificationservice.domain.kafka.consumer.direct.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.haritonenko.commonlibs.dto.kafka.event.NotificationEvent;
import ru.haritonenko.notificationservice.domain.service.DirectNotificationEventProcessor;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationKafkaDirectEventListener {

    private final DirectNotificationEventProcessor eventProcessor;

    @KafkaListener(topics = "${app.kafka.consumer.topics.notification-events}", containerFactory = "directNotificationKafkaListenerContainerFactory")
    public void listenNotificationEvent(ConsumerRecord<UUID, NotificationEvent<?>> record) {
        NotificationEvent<?> event = record.value();
        if (event == null || event.payload() == null) {
            log.warn("Received empty direct notification event in notification-service");
            return;
        }

        eventProcessor.process(event);
    }
}
