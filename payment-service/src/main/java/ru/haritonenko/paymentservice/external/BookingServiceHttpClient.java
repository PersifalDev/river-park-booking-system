package ru.haritonenko.paymentservice.external;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import ru.haritonenko.commonlibs.dto.kafka.event.PaymentKafkaEvent;

@HttpExchange(accept = "application/json", contentType = "application/json")
public interface BookingServiceHttpClient {

    @PostExchange("/api/v1/internal/payment-events")
    void handlePaymentEvent(@RequestBody PaymentKafkaEvent<?> event);
}
