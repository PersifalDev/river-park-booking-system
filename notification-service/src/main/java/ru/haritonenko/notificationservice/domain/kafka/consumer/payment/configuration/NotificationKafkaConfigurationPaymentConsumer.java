package ru.haritonenko.notificationservice.domain.kafka.consumer.payment.configuration;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.UUIDDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import ru.haritonenko.commonlibs.dto.kafka.event.PaymentKafkaEvent;
import ru.haritonenko.commonlibs.dto.kafka.payload.PaymentKafkaPayload;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Configuration
public class NotificationKafkaConfigurationPaymentConsumer {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${app.kafka.consumer.groups.payment-events}")
    private String groupId;

    @Value("${app.kafka.consumer.trusted-packages}")
    private String trustedPackages;

    @Bean
    public ConsumerFactory<UUID, PaymentKafkaEvent<PaymentKafkaPayload>> paymentNotificationConsumerFactoryBean() {
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

    @Bean(name = "paymentKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<UUID, PaymentKafkaEvent<PaymentKafkaPayload>> paymentKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<UUID, PaymentKafkaEvent<PaymentKafkaPayload>> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(paymentNotificationConsumerFactoryBean());
        return factory;
    }
}
