package ru.haritonenko.catalogservice.photo.category.domain.mapper;
import ru.haritonenko.catalogservice.photo.category.api.dto.RoomCategoryPhotoResponseDto;
import ru.haritonenko.catalogservice.photo.category.domain.RoomCategoryPhoto;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RoomCategoryPhotoDomainToResponseDtoMapper {

    @Value("${app.public-base-url}")
    private String publicBaseUrl;

    public RoomCategoryPhotoResponseDto toDto(RoomCategoryPhoto domain) {
        return RoomCategoryPhotoResponseDto.builder()
                .id(domain.id())
                .url(buildPhotoUrl(domain.path()))
                .photoType(domain.photoType().name())
                .build();
    }

    private String buildPhotoUrl(String path) {
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