package ru.haritonenko.bookingservice;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import ru.haritonenko.bookingservice.cache.config.BookingCacheProperties;
import ru.haritonenko.bookingservice.config.inventory.BookingRoomInventoryProperties;
import ru.haritonenko.bookingservice.config.notification.BookingReminderNotificationProperties;
import ru.haritonenko.bookingservice.config.notification.BookingReviewNotificationProperties;
import ru.haritonenko.bookingservice.config.pricing.BookingPriceCalendarProperties;
import ru.haritonenko.bookingservice.config.tariff.BookingTariffProperties;
import ru.haritonenko.bookingservice.config.validation.BookingValidationProperties;
import ru.haritonenko.bookingservice.tasks.domain.async.dispatcher.config.AsyncBookingTaskDispatcherProperties;

import java.util.TimeZone;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableConfigurationProperties({
        BookingCacheProperties.class,
        AsyncBookingTaskDispatcherProperties.class,
        BookingValidationProperties.class,
        BookingTariffProperties.class,
        BookingReviewNotificationProperties.class,
        BookingReminderNotificationProperties.class,
        BookingPriceCalendarProperties.class,
        BookingRoomInventoryProperties.class
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
