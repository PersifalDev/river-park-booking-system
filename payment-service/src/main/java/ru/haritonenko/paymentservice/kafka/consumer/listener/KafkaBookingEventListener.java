package ru.haritonenko.paymentservice.kafka.consumer.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.haritonenko.commonlibs.dto.kafka.event.BookingKafkaEvent;
import ru.haritonenko.commonlibs.dto.kafka.event.type.BookingEventType;
import ru.haritonenko.commonlibs.dto.kafka.payload.BookingKafkaPayload;
import ru.haritonenko.paymentservice.domain.service.PaymentService;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaBookingEventListener {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "${app.kafka.consumer.topics.booking-events}", containerFactory = "bookingPaymentKafkaListenerContainerFactory")
    public void listenBookingEvent(ConsumerRecord<UUID, BookingKafkaEvent<?>> record) {
        BookingKafkaEvent<?> event = record.value();
        if (event == null || event.payload() == null) {
            log.warn("Received empty booking event in payment-service");
            return;
        }

        BookingKafkaPayload payload = objectMapper.convertValue(event.payload(), BookingKafkaPayload.class);
        log.info("Booking event received in payment-service: key={}, eventId={}, eventType={}, payload={}", record.key(), event.eventId(), event.eventType(), payload);

        if (event.eventType() == BookingEventType.BOOKING_HOLD_CREATED) {
            paymentService.createPendingPayment(payload);
            return;
        }
        if (event.eventType() == BookingEventType.BOOKING_CANCELLED || event.eventType() == BookingEventType.BOOKING_EXPIRED || event.eventType() == BookingEventType.BOOKING_FAILED) {
            paymentService.cancelPaymentInternal(payload.bookingId(), payload.cancellationReason());
        }
    }
}
