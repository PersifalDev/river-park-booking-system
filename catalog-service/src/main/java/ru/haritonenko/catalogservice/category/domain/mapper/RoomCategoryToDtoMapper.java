package ru.haritonenko.catalogservice.category.domain.mapper;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.haritonenko.catalogservice.category.api.dto.RoomCategoryResponseDto;
import ru.haritonenko.catalogservice.category.domain.RoomCategory;

@Component
public class RoomCategoryToDtoMapper {

    @Value("${app.public-base-url}")
    private String publicBaseUrl;

    public RoomCategoryResponseDto toDto(RoomCategory domain) {
        return RoomCategoryResponseDto.builder()
                .id(domain.id())
                .name(domain.name())
                .description(domain.description())
                .maxGuests(domain.maxGuests())
                .basePrice(domain.basePrice())
                .areaSquare(domain.areaSquare())
                .totalUnits(domain.totalUnits())
                .mainPhotoUrl(buildPhotoUrl(domain.mainPhotoPath()))
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
