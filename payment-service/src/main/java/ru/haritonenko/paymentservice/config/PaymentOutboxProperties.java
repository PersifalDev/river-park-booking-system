package ru.haritonenko.paymentservice.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.payment.outbox")
public class PaymentOutboxProperties {

    private boolean enabled = true;
    private int batchSize = 10;
    private int maxAttempts = 5;
    private Duration retryDelay = Duration.ofSeconds(30);
    private Duration processingTimeout = Duration.ofSeconds(30);
}
