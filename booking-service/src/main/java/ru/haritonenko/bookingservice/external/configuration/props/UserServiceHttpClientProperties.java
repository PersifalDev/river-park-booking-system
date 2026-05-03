package ru.haritonenko.bookingservice.external.configuration.props;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.external.http-client.user-service")
public class UserServiceHttpClientProperties extends HttpClientProperties {
}