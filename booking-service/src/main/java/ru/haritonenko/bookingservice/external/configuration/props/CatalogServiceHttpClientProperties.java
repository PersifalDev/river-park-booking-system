package ru.haritonenko.bookingservice.external.configuration.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.external.http-client.catalog-service")
public class CatalogServiceHttpClientProperties extends HttpClientProperties {
}