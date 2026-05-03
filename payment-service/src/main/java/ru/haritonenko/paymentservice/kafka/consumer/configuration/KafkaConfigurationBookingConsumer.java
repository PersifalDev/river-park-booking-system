package ru.haritonenko.paymentservice.kafka.consumer.configuration;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.UUIDDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import ru.haritonenko.commonlibs.dto.kafka.event.BookingKafkaEvent;
import ru.haritonenko.commonlibs.dto.kafka.payload.BookingKafkaPayload;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EnableKafka
@Configuration
public class KafkaConfigurationBookingConsumer {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${app.kafka.consumer.groups.booking-events}")
    private String groupId;

    @Value("${app.kafka.consumer.trusted-packages}")
    private String trustedPackages;

    @Bean
    public ConsumerFactory<UUID, BookingKafkaEvent<BookingKafkaPayload>> bookingPaymentConsumerFactory() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configs.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configs.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configs.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        configs.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        configs.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, UUIDDeserializer.class);
        configs.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JsonDeserializer.class);
        configs.put(JsonDeserializer.TRUSTED_PACKAGES, trustedPackages);
        return new DefaultKafkaConsumerFactory<>(configs);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<UUID, BookingKafkaEvent<BookingKafkaPayload>> bookingPaymentKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<UUID, BookingKafkaEvent<BookingKafkaPayload>> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(bookingPaymentConsumerFactory());
        return factory;
    }
}
