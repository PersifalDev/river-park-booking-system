package ru.haritonenko.catalogservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.time.Duration;

@Configuration
public class StaticResourceConfig implements WebMvcConfigurer {

    @Value("${app.base-dir}")
    private String baseDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/**")
                .addResourceLocations(Path.of(baseDir).toUri().toString())
                .setCachePeriod((int) Duration.ofDays(30).toSeconds())
                .resourceChain(true);
    }
}
