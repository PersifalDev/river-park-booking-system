package ru.haritonenko.bookingservice.external.configuration.props;

import lombok.Data;

import java.time.Duration;

@Data
public abstract class HttpClientProperties {
    private String baseUrl;
    private Duration connectTimeout;
    private Duration readTimeout;
}
