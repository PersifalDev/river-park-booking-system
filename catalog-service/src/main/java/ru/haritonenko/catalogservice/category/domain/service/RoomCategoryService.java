package ru.haritonenko.catalogservice.category.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.haritonenko.catalogservice.category.api.dto.filter.RoomCategoryPageFilter;
import ru.haritonenko.catalogservice.category.api.dto.filter.RoomCategorySearchRequestDto;
import ru.haritonenko.catalogservice.config.properties.cache.CacheProperties;
import ru.haritonenko.catalogservice.category.domain.RoomCategory;
import ru.haritonenko.catalogservice.category.domain.db.entity.RoomCategoryEntity;
import ru.haritonenko.catalogservice.category.domain.db.repository.RoomCategoryEntityRepository;
import ru.haritonenko.catalogservice.category.domain.exception.RoomCategoryNotFoundException;
import ru.haritonenko.catalogservice.category.domain.mapper.RoomCategoryEntityToDomainMapper;
import ru.haritonenko.catalogservice.photo.category.domain.RoomCategoryPhoto;
import ru.haritonenko.catalogservice.photo.category.domain.exception.InvalidPathException;
import ru.haritonenko.catalogservice.photo.category.domain.exception.PhotoListNotFoundException;
import ru.haritonenko.catalogservice.photo.category.domain.exception.PhotoNotFoundException;
import ru.haritonenko.catalogservice.photo.category.domain.service.RoomCategoryPhotoService;
import ru.haritonenko.commonlibs.utils.pages.CommonPageable;

import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;
import static java.util.Objects.nonNull;

@Service
@Slf4j
@RequiredArgsConstructor
public class RoomCategoryService {

    private static final String CACHE_KEY_PREFIX = "room-category-:";

    private final RoomCategoryEntityRepository roomCategoryRepository;
    private final RoomCategoryPhotoService photoService;
    private final RoomCategoryEntityToDomainMapper mapper;
    private final RedisTemplate<String, RoomCategory> redisCategoryTemplate;
    private final CacheProperties cacheProperties;

    @Value("${app.roomCategory.default-page-number}")
    private int defaultPageNumber;
    @Value("${app.roomCategory.default-page-size}")
    private int defaultPageSize;

    @Transactional(readOnly = true)
    public Page<RoomCategory> getRoomCategories(RoomCategoryPageFilter pageFilter) {
        log.info("Getting all room categories");

        var pageable = CommonPageable.getPageable(
                pageFilter,
                defaultPageNumber,
                defaultPageSize
        );

        Page<RoomCategoryEntity> categoriesPage = roomCategoryRepository.findAllCategories(pageable);

        return mapCategoriesPageWithMainPhotos(categoriesPage);
    }

    @Transactional(readOnly = true)
    public RoomCategory getRoomCategoryById(Long roomCategoryId) {
        log.info("Getting room category by id={} from cache", roomCategoryId);

        String key = CACHE_KEY_PREFIX + roomCategoryId;
        try{
            var roomCategoryFromCache = redisCategoryTemplate.opsForValue()
                    .get(key);
            if(nonNull(roomCategoryFromCache)){
                log.info("Room with id={}, successfully found from cache",roomCategoryFromCache.id());
                return getRoomCategoryWithMainPhoto(roomCategoryFromCache);
            }
        }
        catch(RedisConnectionFailureException ex){
            log.warn(
                    "Redis unavailable during read, fallback to DB. key={}",
                    key, ex
            );
        }

        log.info("Getting room category by id={} from db", roomCategoryId);
        var roomCategoryEntity = roomCategoryRepository.findCategoryById(roomCategoryId)
                .orElseThrow(() -> {
                    log.warn("Room category with id={} wasn't found", roomCategoryId);
                    return new RoomCategoryNotFoundException("No found room category by id = %s".formatted(roomCategoryId));
                });

        log.info("Room Category with id={} was successfully found from bd", roomCategoryEntity.getId());

        try {
            log.info("Saving room category by id={} in cache", roomCategoryId);
            redisCategoryTemplate.opsForValue().set(
                    key,
                    mapper.toDomain(roomCategoryEntity),
                    cacheProperties.categoriesTtl()
            );
        }
         catch(RedisConnectionFailureException ex){
                log.warn(
                        "Redis unavailable during read, fallback to DB. key={}",
                        key, ex
                );
            }

        var categoryDomain = mapper.toDomain(roomCategoryEntity);

        return getRoomCategoryWithMainPhoto(categoryDomain);
    }

    @Transactional(readOnly = true)
    public Page<RoomCategory> searchRoomCategoriesWithFilter(
            RoomCategorySearchRequestDto requestRoomsWithFilter,
            RoomCategoryPageFilter pageFilter
    ) {
        log.info("Getting room categories with filter");

        checkFilterConstraintsAreValidOrThrow(requestRoomsWithFilter);

        var pageable = CommonPageable.getPageable(
                pageFilter,
                defaultPageNumber,
                defaultPageSize
        );

        var foundEntityCategories = roomCategoryRepository.getRoomCategoriesWithFilter(
                requestRoomsWithFilter.roomType(),
                requestRoomsWithFilter.guests(),
                requestRoomsWithFilter.priceFrom(),
                requestRoomsWithFilter.priceTo(),
                requestRoomsWithFilter.minArea(),
                pageable
        );

        log.info("Filter for searching for rooms category was successfully used");

        return mapCategoriesPageWithMainPhotos(foundEntityCategories);
    }

    private Page<RoomCategory> mapCategoriesPageWithMainPhotos(Page<RoomCategoryEntity> categoriesPage) {
        List<RoomCategoryEntity> categoryEntityList = categoriesPage.getContent();

        List<Long> categoryIds = categoryEntityList.stream()
                .map(RoomCategoryEntity::getId)
                .toList();

        Map<Long, RoomCategoryPhoto> mainPhotoMap = photoService.getMainPhotosByCategoryIds(categoryIds);

        List<RoomCategory> categoryDomainList = categoryEntityList.stream()
                .map(categoryEntity -> toDomainWithMainPhoto(categoryEntity, mainPhotoMap))
                .toList();

        return new PageImpl<>(categoryDomainList, categoriesPage.getPageable(), categoriesPage.getTotalElements());
    }

    private RoomCategory toDomainWithMainPhoto(
            RoomCategoryEntity categoryEntity,
            Map<Long, RoomCategoryPhoto> mainPhotoMap
    ) {
        if (isNull(categoryEntity)) {
            log.warn("Category entity for mapping to domain can't be null");
            throw new IllegalArgumentException("Category is null");
        }

        var categoryDomain = mapper.toDomain(categoryEntity);
        var mainPhoto = mainPhotoMap.get(categoryEntity.getId());

        return categoryDomain.toBuilder()
                .mainPhotoPath(nonNull(mainPhoto) ? mainPhoto.path() : null)
                .build();
    }

    private RoomCategory getRoomCategoryWithMainPhoto(
            RoomCategory category
    ){
        Long categoryId = category.id();
        try {
            RoomCategoryPhoto mainPhoto = photoService.getMainPhotoByCategoryId(categoryId);
            return category.toBuilder()
                    .mainPhotoPath(mainPhoto.path())
                    .build();
        } catch (PhotoNotFoundException | PhotoListNotFoundException | InvalidPathException ex) {
            log.warn("Photo data is invalid or missing for category id={}", categoryId, ex);
            return category.toBuilder()
                    .mainPhotoPath(null)
                    .build();
        }
    }

    private void checkFilterConstraintsAreValidOrThrow(RoomCategorySearchRequestDto filter) {
        if (nonNull(filter.priceTo()) && nonNull(filter.priceFrom())
                && (filter.priceTo().compareTo(filter.priceFrom()) < 0)) {
            log.warn("Error while checking initial and final prices");
            throw new IllegalArgumentException("priceFrom must be less than or equal to priceTo");
        }
    }
}