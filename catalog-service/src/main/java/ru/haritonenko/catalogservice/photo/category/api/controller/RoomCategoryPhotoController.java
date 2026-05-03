package ru.haritonenko.catalogservice.photo.category.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.haritonenko.catalogservice.photo.category.api.dto.RoomCategoryPhotoResponseDto;
import ru.haritonenko.catalogservice.photo.category.api.dto.filter.RoomCategoryPhotoPageFilter;
import ru.haritonenko.catalogservice.photo.category.domain.mapper.RoomCategoryPhotoDomainToResponseDtoMapper;
import ru.haritonenko.catalogservice.photo.category.domain.service.RoomCategoryPhotoService;

@Validated
@RestController
@RequestMapping("/api/v1/catalog/rooms/{categoryId}/photos")
@RequiredArgsConstructor
public class RoomCategoryPhotoController {

    private final RoomCategoryPhotoService photoService;
    private final RoomCategoryPhotoDomainToResponseDtoMapper mapper;

    @GetMapping
    public Page<RoomCategoryPhotoResponseDto> getCategoryPhotos(
            @PathVariable("categoryId") Long categoryId,
            @Valid @ModelAttribute RoomCategoryPhotoPageFilter pageFilter
    ) {
        return photoService.getCategoryPhotos(categoryId, pageFilter)
                .map(mapper::toDto);
    }
}