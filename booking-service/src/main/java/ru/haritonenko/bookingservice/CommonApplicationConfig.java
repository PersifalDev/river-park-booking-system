package ru.haritonenko.bookingservice;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import ru.haritonenko.bookingservice.cache.config.BookingCacheProperties;
import ru.haritonenko.bookingservice.config.tariff.BookingTariffProperties;
import ru.haritonenko.bookingservice.config.validation.BookingValidationProperties;
import ru.haritonenko.bookingservice.tasks.domain.async.dispatcher.config.AsyncBookingTaskDispatcherProperties;

import java.util.TimeZone;
import java.util.concurrent.*;

@Configuration
@EnableConfigurationProperties({
        BookingCacheProperties.class,
        AsyncBookingTaskDispatcherProperties.class,
        BookingValidationProperties.class,
        BookingTariffProperties.class
})
public class CommonApplicationConfig {

    @Bean
    public InitializingBean bookingTimeZoneInitializer(BookingValidationProperties properties) {
        return () -> {
            if (properties.getDateZone() != null) {
                TimeZone.setDefault(TimeZone.getTimeZone(properties.getDateZone()));
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean(RestClient.Builder.class)
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean(destroyMethod = "shutdown")
    public ExecutorService taskDispatcherThreadPool(AsyncBookingTaskDispatcherProperties properties) {
        return new ThreadPoolExecutor(
                properties.getThreadPoolSize(),
                properties.getThreadPoolSize(),
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(properties.getQueueCapacity()),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }

    @Bean(destroyMethod = "shutdown")
    public ExecutorService externalHttpThreadPool(AsyncBookingTaskDispatcherProperties properties) {
        if (Boolean.TRUE.equals(properties.getExternalHttpVirtualThreadsEnabled())) {
            return Executors.newVirtualThreadPerTaskExecutor();
        }
        return new ThreadPoolExecutor(
                properties.getThreadPoolSize(),
                properties.getThreadPoolSize(),
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(properties.getQueueCapacity()),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
    }
}
