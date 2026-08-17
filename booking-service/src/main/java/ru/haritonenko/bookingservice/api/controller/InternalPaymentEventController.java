package ru.haritonenko.bookingservice.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.haritonenko.bookingservice.domain.service.PaymentEventProcessor;
import ru.haritonenko.commonlibs.dto.kafka.event.PaymentEvent;

@RestController
@RequestMapping("/api/v1/internal/payment-events")
@RequiredArgsConstructor
public class InternalPaymentEventController {

    private final PaymentEventProcessor eventProcessor;

    @PostMapping
    public ResponseEntity<Void> handle(@RequestBody PaymentEvent<?> event) {
        eventProcessor.process(event);
        return ResponseEntity.noContent().build();
    }
}
