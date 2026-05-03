package ru.haritonenko.bookingservice.tasks.domain.async.dispatcher.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.booking.task")
public class AsyncBookingTaskDispatcherProperties {

    private Integer threadPoolSize;
    private Integer queueCapacity;
    private Integer maxAttempts;
    private Duration retryDelay;
    private Duration schedulerDelay;
    private Integer dispatchBatchSize;
    private Duration holdTtl;
}
