package ru.haritonenko.bookingservice.external.circuit;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.external.circuit-breaker")
public class ExternalCircuitBreakerProperties {

    private boolean enabled = true;
    private int failureThreshold = 5;
    private Duration openStateDuration = Duration.ofSeconds(30);
}
