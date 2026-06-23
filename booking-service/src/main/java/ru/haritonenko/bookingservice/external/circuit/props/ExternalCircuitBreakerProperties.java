package ru.haritonenko.bookingservice.external.circuit.props;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.external.circuit-breaker")
public class ExternalCircuitBreakerProperties {

    private boolean enabled = true;
    private int failureThreshold;
    private Duration openStateDuration;
}
