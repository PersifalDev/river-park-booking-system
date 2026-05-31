package ru.haritonenko.bookingservice.cache.config;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.booking.cache")
public class BookingCacheProperties {

    @NotNull(message = "Ttl can not be null")
    private Duration pageIndexTtl;
}