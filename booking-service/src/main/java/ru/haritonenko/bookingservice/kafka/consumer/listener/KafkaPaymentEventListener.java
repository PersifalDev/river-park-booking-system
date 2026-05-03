package ru.haritonenko.bookingservice.kafka.consumer.listener;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.haritonenko.bookingservice.domain.db.entity.BookingEntity;
import ru.haritonenko.bookingservice.domain.service.BookingService;
import ru.haritonenko.bookingservice.domain.status.BookingStatus;
import ru.haritonenko.commonlibs.dto.kafka.event.PaymentKafkaEvent;
import ru.haritonenko.commonlibs.dto.kafka.payload.PaymentKafkaPayload;

import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaPaymentEventListener {

    private final BookingService bookingService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${app.kafka.consumer.topics.payment-events}",
            containerFactory = "paymentKafkaListenerContainerFactory"
    )
    public void listenPaymentEvent(ConsumerRecord<UUID, PaymentKafkaEvent<?>> record) {
        PaymentKafkaEvent<?> event = record.value();
        if (event == null || event.payload() == null) {
            log.warn("Received empty payment event in booking-service");
            return;
        }

        PaymentKafkaPayload payload = objectMapper.convertValue(event.payload(), PaymentKafkaPayload.class);
        log.info("Payment event received: key={}, eventId={}, eventType={}, payload={}",
                record.key(),
                event.eventId(),
                event.eventType(),
                payload);

        BookingEntity booking = bookingService.findBookingEntity(payload.bookingId());
        BookingStatus currentStatus = booking.getStatus();

        switch (event.eventType()) {
            case PAYMENT_CONFIRMED -> {
                if (currentStatus == BookingStatus.HOLD) {
                    bookingService.confirmBookingByUuidAndUserId(payload.bookingId(), payload.userId());
                    return;
                }
                log.info("Skip payment confirmation handling because booking already has status={}", currentStatus);
            }
            case PAYMENT_CANCELLED -> {
                if (List.of(BookingStatus.CREATED, BookingStatus.HOLD).contains(currentStatus)) {
                    bookingService.cancelBookingByUuidAndUserId(payload.bookingId(), payload.userId());
                    return;
                }
                log.info("Skip payment cancellation handling because booking already has status={}", currentStatus);
            }
            case PAYMENT_FAILED -> {
                if (!List.of(BookingStatus.CANCELLED, BookingStatus.EXPIRED, BookingStatus.FAILED).contains(currentStatus)) {
                    bookingService.markBookingFailed(payload.bookingId(), payload.cancellationReason());
                    return;
                }
                log.info("Skip payment failure handling because booking already has status={}", currentStatus);
            }
            default -> log.info("Payment event ignored by booking-service: eventType={}", event.eventType());
        }
    }
}
