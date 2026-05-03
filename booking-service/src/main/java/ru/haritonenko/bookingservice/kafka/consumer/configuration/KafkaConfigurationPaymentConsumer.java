package ru.haritonenko.bookingservice.kafka.consumer.configuration;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.UUIDDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import ru.haritonenko.commonlibs.dto.kafka.event.PaymentKafkaEvent;
import ru.haritonenko.commonlibs.dto.kafka.payload.PaymentKafkaPayload;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Configuration
@EnableKafka
public class KafkaConfigurationPaymentConsumer {

    @Value("${app.kafka.consumer.trusted-packages}")
    private String trustedPackages;

    @Bean
    public ConsumerFactory<UUID, PaymentKafkaEvent<PaymentKafkaPayload>> paymentConsumerFactory(
            @Value("${spring.kafka.bootstrap-servers}") String bootstrapServers,
            @Value("${app.kafka.consumer.groups.payment-events}") String groupId
    ) {
        Map<String, Object> configProperties = new HashMap<>();
        configProperties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProperties.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        configProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, UUIDDeserializer.class);
        configProperties.put(JsonDeserializer.TRUSTED_PACKAGES, trustedPackages);
        configProperties.put(JsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        configProperties.put(JsonDeserializer.VALUE_DEFAULT_TYPE, PaymentKafkaEvent.class.getName());

        JsonDeserializer<PaymentKafkaEvent<PaymentKafkaPayload>> jsonDeserializer =
                new JsonDeserializer<>();

        return new DefaultKafkaConsumerFactory<>(
                configProperties,
                new UUIDDeserializer(),
                jsonDeserializer
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<UUID, PaymentKafkaEvent<PaymentKafkaPayload>> paymentKafkaListenerContainerFactory(
            ConsumerFactory<UUID, PaymentKafkaEvent<PaymentKafkaPayload>> paymentConsumerFactory
    ) {
        ConcurrentKafkaListenerContainerFactory<UUID, PaymentKafkaEvent<PaymentKafkaPayload>> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(paymentConsumerFactory);
        return factory;
    }
}