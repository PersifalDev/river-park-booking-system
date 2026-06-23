package ru.haritonenko.bookingservice.kafka.outbox.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.booking.outbox")
public class BookingOutboxProperties {

    private int batchSize;
    private int pageNumber;
    private Duration retryDelay;
    private int maxAttempts;
}
