package ru.haritonenko.paymentservice.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        PaymentWorkModeProperties.class,
        PaymentOutboxProperties.class
})
public class PaymentApplicationConfig {
}
