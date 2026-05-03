package ru.haritonenko.catalogservice.services.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.haritonenko.catalogservice.photo.category.domain.exception.InvalidPathException;
import ru.haritonenko.catalogservice.photo.category.domain.exception.PhotoNotFoundException;
import ru.haritonenko.catalogservice.photo.service.domain.exception.ServiceItemPhotoNotFoundException;
import ru.haritonenko.catalogservice.photo.service.domain.service.ServiceItemPhotoService;
import ru.haritonenko.catalogservice.services.api.dto.filter.ServiceItemPageFilter;
import ru.haritonenko.catalogservice.services.domain.ServiceItem;
import ru.haritonenko.catalogservice.services.domain.db.repository.ServiceItemRepository;
import ru.haritonenko.catalogservice.services.domain.exception.ServiceItemNotFoundException;
import ru.haritonenko.catalogservice.services.domain.mapper.ServiceItemEntityToDomainMapper;
import ru.haritonenko.catalogservice.services.domain.type.ServiceItemType;
import ru.haritonenko.commonlibs.utils.pages.CommonPageable;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@CacheConfig(cacheNames = "services")
public class ServiceItemService {

    private final ServiceItemRepository repository;
    private final ServiceItemEntityToDomainMapper entityToDomainMapper;
    private final ServiceItemPhotoService photoService;

    @Value("${app.roomCategory.default-page-number}")
    private int defaultPageNumber;
    @Value("${app.roomCategory.default-page-size}")
    private int defaultPageSize;

    @Cacheable(value = "servicesPages", key = "(#pageFilter == null || #pageFilter.pageNumber == null ? 0 : #pageFilter.pageNumber) + ':' + (#pageFilter == null || #pageFilter.pageSize == null ? 10 : #pageFilter.pageSize)")
    @Transactional(readOnly = true)
    public List<ServiceItem> getAllActiveServicesWithPageable(ServiceItemPageFilter pageFilter) {
        log.info("Getting all active service items");
        return repository.findByIsActiveTrueOrderBySortOrderAsc(
                        CommonPageable.getPageable(pageFilter, defaultPageNumber, defaultPageSize))
                .stream()
                .map(entityToDomainMapper::map)
                .map(this::withPhoto)
                .toList();
    }

    @Cacheable(key = "#id")
    @Transactional(readOnly = true)
    public ServiceItem getActiveServiceById(Long id) {
        log.info("Getting active service item by id={}", id);
        return repository.findByIdAndIsActiveTrue(id)
                .map(entityToDomainMapper::map)
                .map(this::withPhoto)
                .orElseThrow(() -> new ServiceItemNotFoundException(id));
    }

    @Cacheable(key = "#type")
    @Transactional(readOnly = true)
    public ServiceItem getActiveServiceByType(ServiceItemType type) {
        log.info("Getting active service item by type={}", type);
        return repository.findByTypeAndIsActiveTrue(type)
                .map(entityToDomainMapper::map)
                .map(this::withPhoto)
                .orElseThrow(() -> new ServiceItemNotFoundException(type.getValue()));
    }

    private ServiceItem withPhoto(ServiceItem serviceItem) {
        try {
            var photo = photoService.getByServiceItemId(serviceItem.id());
            return serviceItem.toBuilder().photoPath(photo.path()).build();
        } catch (ServiceItemPhotoNotFoundException | PhotoNotFoundException | InvalidPathException exception) {
            log.warn("Photo for service item with id={} not found or invalid", serviceItem.id(), exception);
            return serviceItem.toBuilder().photoPath(null).build();
        }
    }
}
