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
import ru.haritonenko.bookingservice.external.circuit.props.ExternalCircuitBreakerProperties;
import ru.haritonenko.bookingservice.external.circuit.SimpleCircuitBreaker;
import ru.haritonenko.bookingservice.external.configuration.props.CatalogServiceHttpClientProperties;
import ru.haritonenko.bookingservice.external.configuration.props.HttpClientProperties;
import ru.haritonenko.bookingservice.external.configuration.props.UserServiceHttpClientProperties;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.net.http.HttpClient;

@Configuration
@EnableConfigurationProperties({
        CatalogServiceHttpClientProperties.class,
        UserServiceHttpClientProperties.class,
        ExternalCircuitBreakerProperties.class
})
@RequiredArgsConstructor
public class ExternalBookingConfig {

    private final RestClient.Builder builder;
    private final ExternalCircuitBreakerProperties circuitBreakerProperties;

    @Bean
    public CatalogServiceHttpClient catalogServiceHttpClient(
            CatalogServiceHttpClientProperties props
    ) {
        return createClient(props, CatalogServiceHttpClient.class, "catalog-service");
    }

    @Bean
    public UserServiceHttpClient userServiceHttpClient(
            UserServiceHttpClientProperties props
    ) {
        return createClient(props, UserServiceHttpClient.class, "user-service");
    }

    private <T> T createClient(HttpClientProperties props, Class<T> clientClass, String serviceName) {
        var httpClient = HttpClient.newBuilder()
                .connectTimeout(props.getConnectTimeout())
                .build();

        var requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(props.getReadTimeout());

        var restClient = builder
                .baseUrl(props.getBaseUrl())
                .requestFactory(requestFactory)
                .build();

        T client = HttpServiceProxyFactory
                .builderFor(RestClientAdapter.create(restClient))
                .build()
                .createClient(clientClass);

        if (!circuitBreakerProperties.isEnabled()) {
            return client;
        }
        return wrapWithCircuitBreaker(client, clientClass, serviceName);
    }

    private <T> T wrapWithCircuitBreaker(T client, Class<T> clientClass, String serviceName) {
        SimpleCircuitBreaker circuitBreaker = new SimpleCircuitBreaker(
                serviceName,
                circuitBreakerProperties.getFailureThreshold(),
                circuitBreakerProperties.getOpenStateDuration()
        );
        Object proxy = Proxy.newProxyInstance(
                clientClass.getClassLoader(),
                new Class<?>[]{clientClass},
                (target, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return method.invoke(client, args);
                    }
                    return circuitBreaker.execute(() -> {
                        try {
                            return method.invoke(client, args);
                        } catch (IllegalAccessException ex) {
                            throw new IllegalStateException(ex);
                        } catch (InvocationTargetException ex) {
                            Throwable cause = ex.getCause();
                            if (cause instanceof RuntimeException runtimeException) {
                                throw runtimeException;
                            }
                            throw new IllegalStateException(cause);
                        }
                    });
                }
        );
        return clientClass.cast(proxy);
    }
}
