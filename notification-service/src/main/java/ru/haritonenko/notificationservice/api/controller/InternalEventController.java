package ru.haritonenko.notificationservice.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.haritonenko.commonlibs.dto.kafka.event.BookingKafkaEvent;
import ru.haritonenko.commonlibs.dto.kafka.event.NotificationKafkaEvent;
import ru.haritonenko.commonlibs.dto.kafka.event.PaymentKafkaEvent;
import ru.haritonenko.notificationservice.domain.service.DirectNotificationEventProcessor;
import ru.haritonenko.notificationservice.domain.service.booking.BookingEventProcessor;
import ru.haritonenko.notificationservice.domain.service.payment.PaymentEventProcessor;

@RestController
@RequestMapping("/api/v1/internal/events")
@RequiredArgsConstructor
public class InternalEventController {

    private final BookingEventProcessor bookingEventProcessor;
    private final PaymentEventProcessor paymentEventProcessor;
    private final DirectNotificationEventProcessor directNotificationEventProcessor;

    @PostMapping("/booking")
    public ResponseEntity<Void> handleBooking(@RequestBody BookingKafkaEvent<?> event) {
        bookingEventProcessor.process(event);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/payment")
    public ResponseEntity<Void> handlePayment(@RequestBody PaymentKafkaEvent<?> event) {
        paymentEventProcessor.process(event);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/notification")
    public ResponseEntity<Void> handleNotification(@RequestBody NotificationKafkaEvent<?> event) {
        directNotificationEventProcessor.process(event);
        return ResponseEntity.noContent().build();
    }
}
