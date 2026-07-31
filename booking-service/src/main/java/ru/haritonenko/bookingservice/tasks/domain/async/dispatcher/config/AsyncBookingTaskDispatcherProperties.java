package ru.haritonenko.bookingservice.tasks.domain.async.dispatcher.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.booking.task")
public class AsyncBookingTaskDispatcherProperties {

    private Dispatcher dispatcher = new Dispatcher();
    private ExternalHttp externalHttp = new ExternalHttp();
    private Integer maxAttempts;
    private Duration retryDelay;
    private Duration schedulerDelay;
    private Duration holdTtl;
    private Duration externalCallTimeout;
    private Integer initialAttempts;
    private Duration cleanupRetentionPeriod;

    @Getter
    @Setter
    public static class Dispatcher {
        private Integer threadPoolSize;
        private Integer queueCapacity;
    }

    @Getter
    @Setter
    public static class ExternalHttp {
        private Boolean virtualThreadsEnabled;
        private Integer virtualMaxConcurrency;
        private Platform platform = new Platform();
    }

    @Getter
    @Setter
    public static class Platform {
        private Integer threadPoolSize;
        private Integer queueCapacity;
    }
}
