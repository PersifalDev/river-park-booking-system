package ru.haritonenko.bookingservice.kafka.producer.booking.configuration;

import org.springframework.boot.kafka.autoconfigure.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import ru.haritonenko.commonlibs.dto.kafka.event.BookingEvent;
import ru.haritonenko.commonlibs.dto.kafka.payload.BookingPayload;

import java.util.UUID;

@Configuration
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaConfigurationBookingProducer {

    @Bean
    public KafkaTemplate<UUID, BookingEvent<BookingPayload>> kafkaBookingTemplate(
            KafkaProperties kafkaProperties
    ) {
        var props = kafkaProperties.buildProducerProperties();

        ProducerFactory<UUID, BookingEvent<BookingPayload>> producerFactory =
                new DefaultKafkaProducerFactory<>(props);

        return new KafkaTemplate<>(producerFactory);
    }
}


