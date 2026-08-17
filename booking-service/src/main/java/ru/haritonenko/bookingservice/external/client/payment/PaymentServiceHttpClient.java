package ru.haritonenko.bookingservice.external.client.payment;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import ru.haritonenko.commonlibs.dto.kafka.event.BookingEvent;

@HttpExchange(accept = "application/json", contentType = "application/json")
public interface PaymentServiceHttpClient {

    @PostExchange("/api/v1/internal/booking-events")
    void handleBookingEvent(@RequestBody BookingEvent<?> event);
}
