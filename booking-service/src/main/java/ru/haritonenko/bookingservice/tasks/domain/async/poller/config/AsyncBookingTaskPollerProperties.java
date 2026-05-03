package ru.haritonenko.bookingservice.tasks.domain.async.poller.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "app.task-execution.poller")
public class AsyncBookingTaskPollerProperties {
    private long pollIntervalMs;
    private int batchSize;
    private Duration retryDelay;
    private Integer defaultAttempts;
}
