package ru.haritonenko.catalogservice.config.properties.cache;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.cache")
public record CacheProperties(
        Duration defaultTtl,
        Duration categoriesTtl,
        Duration photosTtl,
        Duration servicesTtl
) {
}