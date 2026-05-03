package ru.haritonenko.catalogservice.photo.service.domain.mapper;

import org.springframework.stereotype.Component;
import ru.haritonenko.catalogservice.photo.service.domain.ServiceItemPhoto;
import ru.haritonenko.catalogservice.photo.service.domain.db.entity.ServiceItemPhotoEntity;

@Component
public class ServiceItemPhotoEntityToDomainMapper {

    public ServiceItemPhoto toDomain(ServiceItemPhotoEntity entity) {
        return ServiceItemPhoto.builder()
                .id(entity.getId())
                .serviceItemId(entity.getServiceItem().getId())
                .path(entity.getPath())
                .build();
    }
}
