package ru.haritonenko.bookingservice.kafka.producer.booking.configuration;

import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.ssl.DefaultSslBundleRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import ru.haritonenko.commonlibs.dto.kafka.event.BookingKafkaEvent;
import ru.haritonenko.commonlibs.dto.kafka.payload.BookingKafkaPayload;

import java.util.UUID;

@Configuration
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaConfigurationBookingProducer {

    @Bean
    public KafkaTemplate<UUID, BookingKafkaEvent<BookingKafkaPayload>> kafkaBookingTemplate(
            KafkaProperties kafkaProperties
    ) {
        var props = kafkaProperties.buildProducerProperties(
                new DefaultSslBundleRegistry()
        );

        ProducerFactory<UUID, BookingKafkaEvent<BookingKafkaPayload>> producerFactory =
                new DefaultKafkaProducerFactory<>(props);

        return new KafkaTemplate<>(producerFactory);
    }
}


