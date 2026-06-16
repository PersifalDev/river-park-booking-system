package ru.haritonenko.catalogservice.photo.category.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.haritonenko.catalogservice.photo.category.api.dto.RoomCategoryPhotoResponseDto;
import ru.haritonenko.catalogservice.photo.category.api.dto.filter.RoomCategoryPhotoPageFilter;
import ru.haritonenko.catalogservice.photo.category.domain.mapper.RoomCategoryPhotoDomainToResponseDtoMapper;
import ru.haritonenko.catalogservice.photo.category.domain.service.RoomCategoryPhotoService;
import ru.haritonenko.commonlibs.utils.pages.PageResponse;

@Validated
@RestController
@RequestMapping("/api/v1/catalog/rooms/{categoryId}/photos")
@RequiredArgsConstructor
@Tag(name = "Room Category Photos", description = "Фотографии категорий номеров")
public class RoomCategoryPhotoController {

    private final RoomCategoryPhotoService photoService;
    private final RoomCategoryPhotoDomainToResponseDtoMapper mapper;

    @GetMapping
    @Operation(summary = "Получить фотографии категории номера")
    @ApiResponse(responseCode = "200", description = "Страница фотографий категории")
    public PageResponse<RoomCategoryPhotoResponseDto> getCategoryPhotos(
            @Parameter(description = "ID категории номера") @PathVariable("categoryId") Long categoryId,
            @Valid @ModelAttribute RoomCategoryPhotoPageFilter pageFilter
    ) {
        return toPageResponse(photoService.getCategoryPhotos(categoryId, pageFilter).map(mapper::toDto));
    }

    private <T> PageResponse<T> toPageResponse(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.getSize(),
                page.getNumber()
        );
    }
}
