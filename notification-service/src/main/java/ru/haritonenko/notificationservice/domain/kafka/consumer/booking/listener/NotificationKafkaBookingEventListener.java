package ru.haritonenko.notificationservice.domain.kafka.consumer.booking.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.haritonenko.commonlibs.dto.kafka.event.BookingEvent;
import ru.haritonenko.notificationservice.domain.service.booking.BookingEventProcessor;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationKafkaBookingEventListener {

    private final BookingEventProcessor eventProcessor;

    @KafkaListener(topics = "${app.kafka.consumer.topics.booking-events}", containerFactory = "bookingNotificationConsumerFactory")
    public void listenBookingEvent(ConsumerRecord<UUID, BookingEvent<?>> record) {
        BookingEvent<?> event = record.value();
        if (event == null || event.payload() == null) {
            log.warn("Received empty booking event in notification-service");
            return;
        }

        log.info("Booking event received in notification-service: key={}, eventId={}, eventType={}",
                record.key(), event.eventId(), event.eventType());
        eventProcessor.process(event);
    }
}
