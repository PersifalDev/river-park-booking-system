package ru.haritonenko.bookingservice.config.openapi;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI bookingOpenAPI(
            @Value("${openapi.title}") String title,
            @Value("${openapi.description}") String description,
            @Value("${openapi.version}") String version,
            @Value("${openapi.security-scheme-name}") String securitySchemeName
    ) {
        return new OpenAPI()
                .info(new Info().title(title).description(description).version(version))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components().addSecuritySchemes(
                        securitySchemeName,
                        new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                ));
    }
}
