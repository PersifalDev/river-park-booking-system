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
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JacksonJsonDeserializer;
import org.springframework.kafka.listener.DefaultErrorHandler;
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
        configProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        configProperties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        configProperties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        configProperties.put(ErrorHandlingDeserializer.KEY_DESERIALIZER_CLASS, UUIDDeserializer.class);
        configProperties.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, JacksonJsonDeserializer.class);
        configProperties.put(JacksonJsonDeserializer.TRUSTED_PACKAGES, trustedPackages);
        configProperties.put(JacksonJsonDeserializer.USE_TYPE_INFO_HEADERS, false);
        configProperties.put(JacksonJsonDeserializer.VALUE_DEFAULT_TYPE, PaymentKafkaEvent.class.getName());

        return new DefaultKafkaConsumerFactory<>(configProperties);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<UUID, PaymentKafkaEvent<PaymentKafkaPayload>> paymentKafkaListenerContainerFactory(
            ConsumerFactory<UUID, PaymentKafkaEvent<PaymentKafkaPayload>> paymentConsumerFactory
            , DefaultErrorHandler bookingKafkaErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<UUID, PaymentKafkaEvent<PaymentKafkaPayload>> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(paymentConsumerFactory);
        factory.setCommonErrorHandler(bookingKafkaErrorHandler);
        return factory;
    }
}
