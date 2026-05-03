package ru.haritonenko.catalogservice.services.domain.mapper;

import org.springframework.stereotype.Component;
import ru.haritonenko.catalogservice.services.domain.ServiceItem;
import ru.haritonenko.catalogservice.services.domain.db.entity.ServiceItemEntity;

@Component
public class ServiceItemEntityToDomainMapper {

    public ServiceItem map(ServiceItemEntity entity) {
        return ServiceItem.builder()
                .id(entity.getId())
                .type(entity.getType())
                .title(entity.getTitle())
                .description(entity.getDescription())
                .isActive(entity.getIsActive())
                .sortOrder(entity.getSortOrder())
                .build();
    }
}
