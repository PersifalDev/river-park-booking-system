package ru.haritonenko.bookingservice.kafka.producer.notification.configuration;


import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import ru.haritonenko.commonlibs.dto.kafka.event.NotificationKafkaEvent;
import ru.haritonenko.commonlibs.dto.kafka.payload.NotificationKafkaPayload;

import java.util.UUID;

@Configuration
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaConfigurationNotificationProducer {

    @Bean
    public KafkaTemplate<UUID, NotificationKafkaEvent<NotificationKafkaPayload>> kafkaNotificationTemplate(
            KafkaProperties kafkaProperties
    ) {
        var props = kafkaProperties.buildProducerProperties(
                new DefaultSslBundleRegistry()
        );

        ProducerFactory<UUID, NotificationKafkaEvent<NotificationKafkaPayload>> producerFactory =
                new DefaultKafkaProducerFactory<>(props);

        return new KafkaTemplate<>(producerFactory);
    }
}
