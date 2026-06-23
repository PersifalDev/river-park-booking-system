package ru.haritonenko.catalogservice.rate.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.rate-limit")
public class CatalogRateLimitProperties {

    private boolean enabled = true;
    private Duration window;
    private int maxRequestsPerWindow;
    private int maxSearchRequestsPerWindow;
    private List<String> excludedPathPrefixes = List.of(
            "/actuator",
            "/swagger-ui",
            "/v3/api-docs",
            "/static",
            "/error"
    );
}
