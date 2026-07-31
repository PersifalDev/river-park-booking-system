package ru.haritonenko.bookingservice;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import ru.haritonenko.bookingservice.cache.config.BookingCacheProperties;
import ru.haritonenko.bookingservice.config.cancellation.BookingCancellationProperties;
import ru.haritonenko.bookingservice.config.code.BookingCodeProperties;
import ru.haritonenko.bookingservice.config.idempotency.BookingIdempotencyProperties;
import ru.haritonenko.bookingservice.config.inventory.BookingRoomInventoryProperties;
import ru.haritonenko.bookingservice.config.notification.BookingReminderNotificationProperties;
import ru.haritonenko.bookingservice.config.notification.BookingReviewNotificationProperties;
import ru.haritonenko.bookingservice.config.page.BookingPageProperties;
import ru.haritonenko.bookingservice.config.pricing.BookingPriceCalendarProperties;
import ru.haritonenko.bookingservice.config.promo.BookingPromoProperties;
import ru.haritonenko.bookingservice.config.tariff.BookingTariffProperties;
import ru.haritonenko.bookingservice.config.validation.BookingValidationProperties;
import ru.haritonenko.bookingservice.config.workmode.BookingWorkModeProperties;
import ru.haritonenko.bookingservice.domain.service.price.props.PricingProperties;
import ru.haritonenko.bookingservice.kafka.outbox.config.BookingOutboxProperties;
import ru.haritonenko.bookingservice.lock.BookingLockProperties;
import ru.haritonenko.bookingservice.tasks.domain.async.dispatcher.config.AsyncBookingTaskDispatcherProperties;
import ru.haritonenko.bookingservice.tasks.domain.async.dispatcher.executor.ConcurrencyLimitedExecutorService;

import java.util.TimeZone;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Configuration
@EnableConfigurationProperties({BookingCacheProperties.class, AsyncBookingTaskDispatcherProperties.class, BookingValidationProperties.class, BookingTariffProperties.class, BookingReviewNotificationProperties.class, BookingReminderNotificationProperties.class, BookingPriceCalendarProperties.class, BookingRoomInventoryProperties.class, BookingPageProperties.class, BookingPromoProperties.class, BookingIdempotencyProperties.class, BookingLockProperties.class, BookingOutboxProperties.class, BookingCancellationProperties.class, BookingCodeProperties.class, PricingProperties.class, BookingWorkModeProperties.class})
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
    public ExecutorService taskDispatcherThreadPool(
            AsyncBookingTaskDispatcherProperties properties,
            MeterRegistry meterRegistry
    ) {
        ExecutorService executor = new ThreadPoolExecutor(
                properties.getDispatcher().getThreadPoolSize(),
                properties.getDispatcher().getThreadPoolSize(),
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(properties.getDispatcher().getQueueCapacity()),
                new ThreadPoolExecutor.CallerRunsPolicy()
        );
        return ExecutorServiceMetrics.monitor(
                meterRegistry,
                executor,
                "booking-task-dispatcher",
                Tags.of("thread.type", "platform")
        );
    }

    @Bean(destroyMethod = "shutdown")
    public ExecutorService externalHttpThreadPool(
            AsyncBookingTaskDispatcherProperties properties,
            MeterRegistry meterRegistry
    ) {
        boolean virtualThreads = Boolean.TRUE.equals(properties.getExternalHttp().getVirtualThreadsEnabled());
        Gauge.builder("booking_external_http_virtual_threads_enabled", () -> virtualThreads ? 1 : 0)
                .description("Whether booking external HTTP executor uses virtual threads")
                .register(meterRegistry);
        ExecutorService executor = virtualThreads
                ? virtualExternalHttpExecutor(properties, meterRegistry)
                : new ThreadPoolExecutor(
                        properties.getExternalHttp().getPlatform().getThreadPoolSize(),
                        properties.getExternalHttp().getPlatform().getThreadPoolSize(),
                        0L,
                        TimeUnit.MILLISECONDS,
                        new ArrayBlockingQueue<>(properties.getExternalHttp().getPlatform().getQueueCapacity()),
                        new ThreadPoolExecutor.CallerRunsPolicy()
                );
        return ExecutorServiceMetrics.monitor(
                meterRegistry,
                executor,
                "booking-external-http",
                Tags.of("thread.type", virtualThreads ? "virtual" : "platform")
        );
    }

    private ExecutorService virtualExternalHttpExecutor(
            AsyncBookingTaskDispatcherProperties properties,
            MeterRegistry meterRegistry
    ) {
        var executor = new ConcurrencyLimitedExecutorService(
                Executors.newVirtualThreadPerTaskExecutor(),
                properties.getExternalHttp().getVirtualMaxConcurrency()
        );
        Gauge.builder("booking_external_http_active_tasks", executor,
                        ConcurrencyLimitedExecutorService::getActiveTaskCount)
                .description("Active tasks in the virtual external HTTP executor")
                .tag("thread.type", "virtual")
                .register(meterRegistry);
        Gauge.builder("booking_external_http_waiting_tasks", executor,
                        ConcurrencyLimitedExecutorService::getWaitingTaskCount)
                .description("Tasks waiting for a virtual external HTTP concurrency permit")
                .tag("thread.type", "virtual")
                .register(meterRegistry);
        Gauge.builder("booking_external_http_max_concurrency", executor,
                        ConcurrencyLimitedExecutorService::getMaxConcurrency)
                .description("Maximum external HTTP task concurrency")
                .tag("thread.type", "virtual")
                .register(meterRegistry);
        return executor;
    }
}
