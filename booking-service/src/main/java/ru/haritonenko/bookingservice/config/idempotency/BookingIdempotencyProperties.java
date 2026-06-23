package ru.haritonenko.bookingservice.config.idempotency;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.booking.idempotency")
public class BookingIdempotencyProperties {

    private Duration ttl;
    private int maxKeyLength;
}
