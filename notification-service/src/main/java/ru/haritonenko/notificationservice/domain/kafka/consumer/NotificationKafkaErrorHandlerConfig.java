package ru.haritonenko.notificationservice.domain.kafka.consumer;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.UUIDSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Configuration
public class NotificationKafkaErrorHandlerConfig {

    @Bean
    public KafkaTemplate<UUID, Object> notificationDltKafkaTemplate(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers
    ) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, UUIDSerializer.class);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);
        return new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(properties));
    }

    @Bean
    public DefaultErrorHandler notificationKafkaErrorHandler(
            KafkaTemplate<UUID, Object> notificationDltKafkaTemplate,
            @Value("${app.kafka.consumer.retry.backoff-ms:1000}") long backoffMs,
            @Value("${app.kafka.consumer.retry.max-retries:3}") long maxRetries
    ) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                notificationDltKafkaTemplate,
                (record, exception) -> new TopicPartition(record.topic() + ".DLT", record.partition())
        );
        return new DefaultErrorHandler(recoverer, new FixedBackOff(backoffMs, maxRetries));
    }
}
