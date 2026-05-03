package ru.haritonenko.bookingservice.external.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;
import ru.haritonenko.bookingservice.external.client.catalog.CatalogServiceHttpClient;
import ru.haritonenko.bookingservice.external.client.users.UserServiceHttpClient;
import ru.haritonenko.bookingservice.external.configuration.props.CatalogServiceHttpClientProperties;
import ru.haritonenko.bookingservice.external.configuration.props.HttpClientProperties;
import ru.haritonenko.bookingservice.external.configuration.props.UserServiceHttpClientProperties;

import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties({
        CatalogServiceHttpClientProperties.class,
        UserServiceHttpClientProperties.class
})
@RequiredArgsConstructor
public class ExternalBookingConfig {

    private final RestClient.Builder builder;

    @Bean
    public CatalogServiceHttpClient catalogServiceHttpClient(
            CatalogServiceHttpClientProperties props
    ) {
        return createClient(props, CatalogServiceHttpClient.class);
    }

    @Bean
    public UserServiceHttpClient userServiceHttpClient(
            UserServiceHttpClientProperties props
    ) {
        return createClient(props, UserServiceHttpClient.class);
    }

    private <T> T createClient(HttpClientProperties props, Class<T> clientClass) {
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(props.getConnectTimeout())
                .build();

        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(props.getReadTimeout());

        var restClient = builder
                .baseUrl(props.getBaseUrl())
                .requestFactory(requestFactory)
                .build();

        return HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(clientClass);
    }
}