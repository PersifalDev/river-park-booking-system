package ru.haritonenko.paymentservice.kafka.consumer.configuration;

import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;
import ru.haritonenko.commonlibs.dto.kafka.event.PaymentEvent;
import ru.haritonenko.commonlibs.dto.kafka.payload.PaymentPayload;

import java.util.UUID;

@Configuration
public class KafkaConsumerErrorHandlerConfig {

    @Bean
    public DefaultErrorHandler paymentKafkaErrorHandler(
            KafkaTemplate<UUID, PaymentEvent<PaymentPayload>> template,
            @Value("${app.kafka.consumer.retry.backoff-ms:1000}") long backoffMs,
            @Value("${app.kafka.consumer.retry.max-retries:3}") long maxRetries
    ) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                template,
                (record, exception) -> new TopicPartition(record.topic() + ".DLT", record.partition())
        );
        return new DefaultErrorHandler(recoverer, new FixedBackOff(backoffMs, maxRetries));
    }
}
