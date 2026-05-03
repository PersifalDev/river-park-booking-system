package ru.haritonenko.telegrambot.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;
import org.telegram.telegrambots.client.okhttp.OkHttpTelegramClient;
import org.telegram.telegrambots.meta.generics.TelegramClient;

@Configuration
@EnableConfigurationProperties({
        BotProperties.class,
        CatalogClientProperties.class,
        BookingClientProperties.class,
        PaymentClientProperties.class,
        UserClientProperties.class,
        NotificationClientProperties.class
})
public class AppConfig {

    @Bean
    public TelegramClient telegramClient(BotProperties properties) {
        return new OkHttpTelegramClient(properties.token());
    }

    @Bean
    public RestClient catalogRestClient(CatalogClientProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .build();
    }

    @Bean
    public RestClient bookingRestClient(BookingClientProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .build();
    }

    @Bean
    public RestClient paymentRestClient(PaymentClientProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .build();
    }

    @Bean
    public RestClient userRestClient(UserClientProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .build();
    }

    @Bean
    public RestClient notificationRestClient(NotificationClientProperties properties) {
        return RestClient.builder()
                .baseUrl(properties.baseUrl())
                .build();
    }
}
