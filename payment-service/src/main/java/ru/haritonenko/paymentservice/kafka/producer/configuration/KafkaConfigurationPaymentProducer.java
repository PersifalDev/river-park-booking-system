package ru.haritonenko.paymentservice.kafka.producer.configuration;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.UUIDSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import ru.haritonenko.commonlibs.dto.kafka.event.PaymentKafkaEvent;
import ru.haritonenko.commonlibs.dto.kafka.payload.PaymentKafkaPayload;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Configuration
public class KafkaConfigurationPaymentProducer {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<UUID, PaymentKafkaEvent<PaymentKafkaPayload>> paymentKafkaProducerFactory() {
        Map<String, Object> configs = new HashMap<>();
        configs.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configs.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, UUIDSerializer.class);
        configs.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(configs);
    }

    @Bean
    public KafkaTemplate<UUID, PaymentKafkaEvent<PaymentKafkaPayload>> paymentKafkaTemplate() {
        return new KafkaTemplate<>(paymentKafkaProducerFactory());
    }
}
