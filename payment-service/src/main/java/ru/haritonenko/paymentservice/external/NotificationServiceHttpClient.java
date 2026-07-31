package ru.haritonenko.paymentservice.external;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import ru.haritonenko.commonlibs.dto.kafka.event.PaymentKafkaEvent;

@HttpExchange(accept = "application/json", contentType = "application/json")
public interface NotificationServiceHttpClient {

    @PostExchange("/api/v1/internal/events/payment")
    void handlePaymentEvent(@RequestBody PaymentKafkaEvent<?> event);
}
