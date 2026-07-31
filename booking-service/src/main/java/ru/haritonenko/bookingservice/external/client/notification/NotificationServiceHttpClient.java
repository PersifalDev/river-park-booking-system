package ru.haritonenko.bookingservice.external.client.notification;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import ru.haritonenko.commonlibs.dto.kafka.event.BookingKafkaEvent;
import ru.haritonenko.commonlibs.dto.kafka.event.NotificationKafkaEvent;

@HttpExchange(accept = "application/json", contentType = "application/json")
public interface NotificationServiceHttpClient {

    @PostExchange("/api/v1/internal/events/booking")
    void handleBookingEvent(@RequestBody BookingKafkaEvent<?> event);

    @PostExchange("/api/v1/internal/events/notification")
    void handleNotificationEvent(@RequestBody NotificationKafkaEvent<?> event);
}
