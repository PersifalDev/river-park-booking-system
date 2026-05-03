package ru.haritonenko.bookingservice;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.support.TransactionTemplate;
import ru.haritonenko.bookingservice.tasks.domain.async.dispatcher.config.AsyncBookingTaskDispatcherProperties;

import java.util.concurrent.*;

@Configuration
@EnableConfigurationProperties(AsyncBookingTaskDispatcherProperties.class)
public class CommonApplicationConfig {

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
