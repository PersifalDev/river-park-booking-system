package ru.haritonenko.paymentservice.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.haritonenko.commonlibs.dto.kafka.event.BookingKafkaEvent;
import ru.haritonenko.commonlibs.dto.kafka.event.type.BookingEventType;
import ru.haritonenko.commonlibs.dto.kafka.payload.BookingKafkaPayload;
import ru.haritonenko.paymentservice.inbox.ProcessedEventService;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingEventProcessor {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;
    private final ProcessedEventService processedEventService;

    public void process(BookingKafkaEvent<?> event) {
        if (event == null || event.payload() == null) {
            throw new IllegalArgumentException("Booking event and payload are required");
        }
        processedEventService.processOnce(
                event.eventId(),
                "payment-service:booking-events",
                () -> processNewEvent(event)
        );
    }

    private void processNewEvent(BookingKafkaEvent<?> event) {
        BookingKafkaPayload payload = objectMapper.convertValue(event.payload(), BookingKafkaPayload.class);
        log.info("Processing booking event in payment-service: eventId={}, eventType={}, bookingId={}",
                event.eventId(),
                event.eventType(),
                payload.bookingId());

        if (event.eventType() == BookingEventType.BOOKING_HOLD_CREATED) {
            paymentService.createPendingPayment(payload);
            return;
        }
        if (event.eventType() == BookingEventType.BOOKING_CANCELLED
                || event.eventType() == BookingEventType.BOOKING_EXPIRED
                || event.eventType() == BookingEventType.BOOKING_FAILED) {
            paymentService.cancelPaymentInternal(payload.bookingId(), payload.cancellationReason());
        }
    }
}
