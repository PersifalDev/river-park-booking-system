package ru.haritonenko.paymentservice.api.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.haritonenko.commonlibs.dto.kafka.event.BookingEvent;
import ru.haritonenko.paymentservice.domain.service.BookingEventProcessor;

@RestController
@RequestMapping("/api/v1/internal/booking-events")
@RequiredArgsConstructor
public class InternalBookingEventController {

    private final BookingEventProcessor eventProcessor;

    @PostMapping
    public ResponseEntity<Void> handle(@RequestBody BookingEvent<?> event) {
        eventProcessor.process(event);
        return ResponseEntity.noContent().build();
    }
}
