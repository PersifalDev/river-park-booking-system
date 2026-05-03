package ru.haritonenko.bookingservice.kafka.producer.booking.sender;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ru.haritonenko.commonlibs.dto.kafka.event.BookingKafkaEvent;
import ru.haritonenko.commonlibs.dto.kafka.payload.BookingKafkaPayload;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaBookingEventSender {

    @Value("${app.kafka.producer.topics.booking-events}")
    private String topic;

    private final KafkaTemplate<UUID, BookingKafkaEvent<BookingKafkaPayload>> kafkaBookingTemplate;

    public void sendEvent(BookingKafkaEvent<BookingKafkaPayload> event) {
        UUID key = event.payload().bookingId();

        log.info("Sending booking event: eventId={}, eventType={}, bookingId={}",
                event.eventId(),
                event.eventType(),
                key);

        kafkaBookingTemplate.send(topic, key, event)
                .whenComplete((sendResult, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send booking event: eventId={}, bookingId={}",
                                event.eventId(),
                                key,
                                ex);
                        return;
                    }

                    log.info("Booking event sent successfully: topic={}, key={}, partition={}, offset={}",
                            topic,
                            key,
                            sendResult.getRecordMetadata().partition(),
                            sendResult.getRecordMetadata().offset());
                });
    }
}