package ru.haritonenko.catalogservice.photo.category.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import ru.haritonenko.catalogservice.photo.category.api.dto.filter.RoomCategoryPhotoPageFilter;
import ru.haritonenko.catalogservice.photo.category.domain.RoomCategoryPhoto;
import ru.haritonenko.catalogservice.photo.category.domain.db.entity.RoomCategoryPhotoEntity;
import ru.haritonenko.catalogservice.photo.category.domain.db.repository.RoomCategoryPhotoEntityRepository;
import ru.haritonenko.catalogservice.photo.category.domain.exception.InvalidPathException;
import ru.haritonenko.catalogservice.photo.category.domain.exception.PhotoListNotFoundException;
import ru.haritonenko.catalogservice.photo.category.domain.exception.PhotoNotFoundException;
import ru.haritonenko.catalogservice.photo.category.domain.mapper.RoomCategoryPhotoEntityToDomainMapper;
import ru.haritonenko.catalogservice.photo.category.domain.type.RoomCategoryPhotoType;
import ru.haritonenko.catalogservice.photo.category.loader.RoomCategoryPhotoLoader;
import ru.haritonenko.catalogservice.config.properties.cache.CacheProperties;
import ru.haritonenko.commonlibs.utils.pages.CommonPageable;

import java.nio.file.Path;
import java.util.*;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoomCategoryPhotoService {

    private static final String CACHE_KEY_PREFIX = "room-category-main-photo:";

    private final RoomCategoryPhotoLoader loader;
    private final RoomCategoryPhotoEntityToDomainMapper mapper;
    private final RoomCategoryPhotoEntityRepository photoRepository;
    private final RedisTemplate<String, RoomCategoryPhoto> redisTemplate;
    private final CacheProperties cacheProperties;

    @Value("${app.roomCategory.default-page-number}")
    private int defaultPageNumber;
    @Value("${app.roomCategory.default-page-size}")
    private int defaultPageSize;

    public RoomCategoryPhoto getMainPhotoByCategoryId(Long categoryId) {
        checkCategoryIdNotNullOrThrow(categoryId);

        String key = CACHE_KEY_PREFIX + categoryId;

        try {
            log.info("Extracting main photo for category with id={} from cache", categoryId);

            var cachedPhoto = redisTemplate.opsForValue().get(key);
            if (nonNull(cachedPhoto)) {
                log.info("Main photo for category with id={} successfully found from cache", categoryId);
                return cachedPhoto;
            }

            log.warn("Main photo for category with id={} not found from cache", categoryId);
        } catch (RedisConnectionFailureException ex) {
            log.warn("Redis unavailable during read, fallback to DB. key={}", key, ex);
        }

        log.info("Extracting main photo for category with id={} from db", categoryId);

        var categoryPhotoList = photoRepository.findByRoomCategoryIdOrderByIdAsc(categoryId);

        if (isNull(categoryPhotoList) || categoryPhotoList.isEmpty()) {
            log.warn("Photo list is empty or missing for category with id={}", categoryId);
            throw new PhotoListNotFoundException("Photo list not found");
        }

        var mainPhotoEntity = pickMainPhotoEntity(categoryPhotoList)
                .orElseThrow(() -> {
                    log.warn("No photo was found in category={}", categoryId);
                    return new PhotoNotFoundException("No photo in category was found");
                });

        log.info("Main photo for category with id={} was successfully found from db", categoryId);

        var updatedPhotoWithValidatedPath = updatePhotoWithValidatedPath(mapper.toDomain(mainPhotoEntity));

        log.info("The path of main photo for category with id={} was updated", categoryId);

        try {
            log.info("Saving main photo for category with id={} in cache", categoryId);
            redisTemplate.opsForValue().set(
                    key,
                    updatedPhotoWithValidatedPath,
                    cacheProperties.photosTtl()
            );
        } catch (RedisConnectionFailureException ex) {
            log.warn("Redis unavailable during write, key={}", key, ex);
        }

        return updatedPhotoWithValidatedPath;
    }

    public Page<RoomCategoryPhoto> getCategoryPhotos(
            Long categoryId,
            RoomCategoryPhotoPageFilter pageFilter
    ) {
        checkCategoryIdNotNullOrThrow(categoryId);

        log.info("Extracting all photos for category id={}", categoryId);

        var photoPage = photoRepository.findByRoomCategoryIdOrderByIdAsc(
                categoryId,
                CommonPageable.getPageable(pageFilter, defaultPageNumber, defaultPageSize)
        );

        if (photoPage.isEmpty()) {
            log.warn("Photo list is empty for category with id={}", categoryId);
            throw new PhotoListNotFoundException("Photo list not found");
        }

        return photoPage.map(mapper::toDomain)
                .map(this::updatePhotoWithValidatedPath);
    }

    public Map<Long, RoomCategoryPhoto> getMainPhotosByCategoryIds(List<Long> categoryIds) {
        if (isNull(categoryIds) || categoryIds.isEmpty()) {
            log.warn("Category ids are empty or missing");
            return Map.of();
        }

        log.info("Extracting main photos for categories count={}", categoryIds.size());

        var photoEntities = photoRepository.findByRoomCategory_IdInOrderByRoomCategory_IdAscIdAsc(categoryIds);

        if (isNull(photoEntities) || photoEntities.isEmpty()) {
            log.warn("No photos were found for categories count={}", categoryIds.size());
            return Map.of();
        }

        Map<Long, List<RoomCategoryPhotoEntity>> photoMap = photoEntities.stream()
                .filter(Objects::nonNull)
                .filter(photo -> nonNull(photo.getRoomCategory()))
                .collect(LinkedHashMap::new,
                        (map, photo) -> map.computeIfAbsent(photo.getRoomCategory().getId(), key -> new ArrayList<>()).add(photo),
                        Map::putAll);

        Map<Long, RoomCategoryPhoto> result = new LinkedHashMap<>();

        for (Long categoryId : categoryIds) {
            var categoryPhotoList = photoMap.get(categoryId);

            if (isNull(categoryPhotoList) || categoryPhotoList.isEmpty()) {
                continue;
            }

            pickMainPhotoEntity(categoryPhotoList)
                    .map(mapper::toDomain)
                    .map(this::updatePhotoWithValidatedPathSafe)
                    .ifPresent(photo -> result.put(categoryId, photo));
        }

        return result;
    }

    private Optional<RoomCategoryPhotoEntity> pickMainPhotoEntity(List<RoomCategoryPhotoEntity> categoryPhotoList) {
        return categoryPhotoList.stream()
                .filter(Objects::nonNull)
                .min(Comparator.comparingInt(photo -> getPhotoPriority(photo.getPhotoType())));
    }

    private int getPhotoPriority(RoomCategoryPhotoType photoType) {
        if (RoomCategoryPhotoType.MAIN.equals(photoType)) {
            return 0;
        }
        if (RoomCategoryPhotoType.DEFAULT.equals(photoType)) {
            return 1;
        }
        return 2;
    }

    private RoomCategoryPhoto updatePhotoWithValidatedPathSafe(RoomCategoryPhoto photo) {
        try {
            return updatePhotoWithValidatedPath(photo);
        } catch (PhotoNotFoundException | InvalidPathException exception) {
            log.warn("Invalid path for photo with id={}", photo.id(), exception);
            return null;
        }
    }

    private RoomCategoryPhoto updatePhotoWithValidatedPath(RoomCategoryPhoto photo) {
        if (isNull(photo)) {
            log.warn("Photo not found to update path");
            throw new IllegalArgumentException("Photo to update can't be null");
        }

        Path validatedPath = getValidatedRelativePath(photo.path());

        return photo.toBuilder()
                .path(loader.getUnixStylePath(validatedPath))
                .build();
    }

    private Path getValidatedRelativePath(String path) {
        if (isNull(path) || path.isBlank()) {
            log.warn("Incorrect path to validate");
            throw new InvalidPathException("Incorrect path");
        }

        log.info("Validating path={}", path);
        return loader.validateRelativePath(path);
    }

    private void checkCategoryIdNotNullOrThrow(Long categoryId) {
        if (isNull(categoryId)) {
            log.warn("Category id for extracting photo is null");
            throw new IllegalArgumentException("Category id is null");
        }
    }
}