package ru.haritonenko.paymentservice.kafka.consumer.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.haritonenko.commonlibs.dto.kafka.event.BookingKafkaEvent;
import ru.haritonenko.paymentservice.domain.service.BookingEventProcessor;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaBookingEventListener {

    private final BookingEventProcessor eventProcessor;

    @KafkaListener(topics = "${app.kafka.consumer.topics.booking-events}", containerFactory = "bookingPaymentKafkaListenerContainerFactory")
    public void listenBookingEvent(ConsumerRecord<UUID, BookingKafkaEvent<?>> record) {
        BookingKafkaEvent<?> event = record.value();
        if (event == null || event.payload() == null) {
            log.warn("Received empty booking event in payment-service");
            return;
        }

        log.info("Booking event received in payment-service: key={}, eventId={}, eventType={}",
                record.key(), event.eventId(), event.eventType());
        eventProcessor.process(event);
    }
}
