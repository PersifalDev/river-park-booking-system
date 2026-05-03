package ru.haritonenko.catalogservice.services.domain.mapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.haritonenko.catalogservice.services.api.dto.ServiceItemResponseDto;
import ru.haritonenko.catalogservice.services.domain.ServiceItem;

@Component
public class ServiceItemToDtoMapper {

    @Value("${app.public-base-url}")
    private String publicBaseUrl;

    public ServiceItemResponseDto toDto(ServiceItem serviceItem) {
        return ServiceItemResponseDto.builder()
                .id(serviceItem.id())
                .type(serviceItem.type() == null ? null : serviceItem.type().getValue())
                .title(serviceItem.title())
                .description(serviceItem.description())
                .photoUrl(buildPhotoUrl(serviceItem.photoPath()))
                .build();
    }

    private String buildPhotoUrl(String path) {
        if (path == null || path.isBlank()) {
            return null;
        }
        String normalizedPath = path;
        if (normalizedPath.startsWith("data/static/")) {
            normalizedPath = normalizedPath.substring("data/static/".length());
        }
        if (normalizedPath.startsWith("/")) {
            normalizedPath = normalizedPath.substring(1);
        }
        return publicBaseUrl + "/static/" + normalizedPath;
    }
}
