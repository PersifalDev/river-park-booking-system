package ru.haritonenko.bookingservice.domain.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import ru.haritonenko.bookingservice.domain.db.entity.BookingEntity;
import ru.haritonenko.bookingservice.domain.status.BookingStatus;
import ru.haritonenko.bookingservice.inbox.ProcessedEventService;
import ru.haritonenko.commonlibs.dto.kafka.event.PaymentEvent;
import ru.haritonenko.commonlibs.dto.kafka.payload.PaymentPayload;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentEventProcessor {

    private final BookingService bookingService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final ProcessedEventService processedEventService;

    public void process(PaymentEvent<?> event) {
        if (event == null || event.payload() == null) {
            throw new IllegalArgumentException("Payment event and payload are required");
        }
        processedEventService.processOnce(
                event.eventId(),
                "booking-service:payment-events",
                () -> processNewEvent(event)
        );
    }

    private void processNewEvent(PaymentEvent<?> event) {
        PaymentPayload payload = objectMapper.convertValue(event.payload(), PaymentPayload.class);
        log.info("Processing payment event in booking-service: eventId={}, eventType={}, bookingId={}",
                event.eventId(),
                event.eventType(),
                payload.bookingId());

        BookingEntity booking = transactionTemplate.execute(status ->
                bookingService.findBookingEntity(payload.bookingId()));
        if (booking == null) {
            throw new IllegalStateException("Booking lookup transaction returned null");
        }
        BookingStatus currentStatus = booking.getStatus();

        switch (event.eventType()) {
            case PAYMENT_CONFIRMED -> {
                if (currentStatus == BookingStatus.HOLD) {
                    bookingService.confirmBookingByUuidAndUserId(payload.bookingId(), payload.userId());
                    return;
                }
                log.info("Skip payment confirmation because booking already has status={}", currentStatus);
            }
            case PAYMENT_CANCELLED -> {
                if (List.of(BookingStatus.CREATED, BookingStatus.HOLD).contains(currentStatus)) {
                    bookingService.cancelBookingByUuidAndUserId(payload.bookingId(), payload.userId());
                    return;
                }
                log.info("Skip payment cancellation because booking already has status={}", currentStatus);
            }
            case PAYMENT_FAILED -> {
                if (!List.of(BookingStatus.CANCELLED, BookingStatus.EXPIRED, BookingStatus.FAILED).contains(currentStatus)) {
                    bookingService.markBookingFailed(payload.bookingId(), payload.cancellationReason());
                    return;
                }
                log.info("Skip payment failure because booking already has status={}", currentStatus);
            }
            default -> log.info("Payment event ignored by booking-service: eventType={}", event.eventType());
        }
    }
}
