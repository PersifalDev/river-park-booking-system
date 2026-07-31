package ru.haritonenko.paymentservice.external;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import ru.haritonenko.commonlibs.security.internal.InternalServiceAuthFilter;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class ExternalPaymentConfig {

    @Bean
    @ConditionalOnMissingBean(RestClient.Builder.class)
    public RestClient.Builder restClientBuilder() {
        return RestClient.builder();
    }

    @Bean
    public BookingServiceHttpClient bookingServiceHttpClient(
            RestClient.Builder builder,
            @Value("${app.external.http-client.booking-service.base-url}") String baseUrl,
            @Value("${app.external.http-client.booking-service.connect-timeout:3s}") Duration connectTimeout,
            @Value("${app.external.http-client.booking-service.read-timeout:5s}") Duration readTimeout,
            @Value("${app.security.internal-service.token:}") String internalToken
    ) {
        return createClient(builder, baseUrl, connectTimeout, readTimeout, internalToken, BookingServiceHttpClient.class);
    }

    @Bean
    public NotificationServiceHttpClient notificationServiceHttpClient(
            RestClient.Builder builder,
            @Value("${app.external.http-client.notification-service.base-url}") String baseUrl,
            @Value("${app.external.http-client.notification-service.connect-timeout:3s}") Duration connectTimeout,
            @Value("${app.external.http-client.notification-service.read-timeout:5s}") Duration readTimeout,
            @Value("${app.security.internal-service.token:}") String internalToken
    ) {
        return createClient(builder, baseUrl, connectTimeout, readTimeout, internalToken, NotificationServiceHttpClient.class);
    }

    private <T> T createClient(
            RestClient.Builder builder,
            String baseUrl,
            Duration connectTimeout,
            Duration readTimeout,
            String internalToken,
            Class<T> type
    ) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        RestClient restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader(InternalServiceAuthFilter.INTERNAL_SERVICE_TOKEN_HEADER, internalToken)
                .requestFactory(requestFactory)
                .build();
        return HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(type);
    }
}
