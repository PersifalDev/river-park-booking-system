package ru.haritonenko.catalogservice.services.domain;

import lombok.Builder;
import ru.haritonenko.catalogservice.services.domain.type.ServiceItemType;

@Builder(toBuilder = true)
public record ServiceItem(
        Long id,
        ServiceItemType type,
        String title,
        String description,
        Boolean isActive,
        Integer sortOrder,
        String photoPath
) {
}
