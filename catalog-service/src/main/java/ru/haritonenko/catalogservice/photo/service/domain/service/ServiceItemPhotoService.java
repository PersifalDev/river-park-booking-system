package ru.haritonenko.catalogservice.photo.service.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import ru.haritonenko.catalogservice.config.properties.cache.CacheProperties;
import ru.haritonenko.catalogservice.photo.category.domain.exception.InvalidPathException;
import ru.haritonenko.catalogservice.photo.category.domain.exception.PhotoNotFoundException;
import ru.haritonenko.catalogservice.photo.service.domain.ServiceItemPhoto;
import ru.haritonenko.catalogservice.photo.service.domain.db.repository.ServiceItemPhotoEntityRepository;
import ru.haritonenko.catalogservice.photo.service.domain.exception.ServiceItemPhotoNotFoundException;
import ru.haritonenko.catalogservice.photo.service.domain.mapper.ServiceItemPhotoEntityToDomainMapper;
import ru.haritonenko.catalogservice.photo.service.loader.ServiceItemPhotoLoader;

import java.nio.file.Path;

@Service
@Slf4j
@RequiredArgsConstructor
public class ServiceItemPhotoService {

    private static final String CACHE_KEY_PREFIX = "service-item-photo:";

    private final ServiceItemPhotoEntityRepository repository;
    private final ServiceItemPhotoEntityToDomainMapper mapper;
    private final ServiceItemPhotoLoader loader;
    private final RedisTemplate<String, ServiceItemPhoto> redisTemplate;
    private final CacheProperties cacheProperties;

    public ServiceItemPhoto getByServiceItemId(Long serviceItemId) {
        if (serviceItemId == null) {
            throw new IllegalArgumentException("Service item id is null");
        }

        String key = CACHE_KEY_PREFIX + serviceItemId;

        try {
            log.info("Extracting photo for service item with id={} from cache", serviceItemId);
            ServiceItemPhoto cachedPhoto = redisTemplate.opsForValue().get(key);
            if (cachedPhoto != null) {
                log.info("Photo for service item with id={} was successfully found from cache", serviceItemId);
                return cachedPhoto;
            }
            log.warn("Photo for service item with id={} not found from cache", serviceItemId);
        } catch (RedisConnectionFailureException exception) {
            log.warn("Redis unavailable during service photo read, fallback to DB. key={}", key, exception);
        }

        var entity = repository.findByServiceItem_Id(serviceItemId)
                .orElseThrow(() -> new ServiceItemPhotoNotFoundException(serviceItemId));

        ServiceItemPhoto validatedPhoto = updatePhotoPath(mapper.toDomain(entity));

        try {
            log.info("Saving photo for service item with id={} in cache", serviceItemId);
            redisTemplate.opsForValue().set(key, validatedPhoto, cacheProperties.photosTtl());
        } catch (RedisConnectionFailureException exception) {
            log.warn("Redis unavailable during service photo write, key={}", key, exception);
        }

        return validatedPhoto;
    }

    private ServiceItemPhoto updatePhotoPath(ServiceItemPhoto photo) {
        if (photo == null) {
            throw new IllegalArgumentException("Photo is null");
        }
        Path validatedPath = getValidatedRelativePath(photo.path());
        return photo.toBuilder()
                .path(loader.getUnixStylePath(validatedPath))
                .build();
    }

    private Path getValidatedRelativePath(String path) {
        if (path == null || path.isBlank()) {
            throw new InvalidPathException("Incorrect path");
        }
        return loader.validateRelativePath(path);
    }
}
