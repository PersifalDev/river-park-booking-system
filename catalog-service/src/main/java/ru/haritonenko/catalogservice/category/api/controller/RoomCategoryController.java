package ru.haritonenko.catalogservice.category.api.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.haritonenko.catalogservice.category.api.dto.RoomCategoryResponseDto;
import ru.haritonenko.catalogservice.category.api.dto.filter.RoomCategoryPageFilter;
import ru.haritonenko.catalogservice.category.api.dto.filter.RoomCategorySearchRequestDto;
import ru.haritonenko.catalogservice.category.domain.mapper.RoomCategoryToDtoMapper;
import ru.haritonenko.catalogservice.category.domain.service.RoomCategoryService;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/catalog/rooms")
@RequiredArgsConstructor
public class RoomCategoryController {

    private final RoomCategoryService roomCategoryService;
    private final RoomCategoryToDtoMapper mapper;

    @GetMapping
    public Page<RoomCategoryResponseDto> getAllRoomCategories(
            @Valid RoomCategoryPageFilter pageFilter
    ) {
        log.info("Request to get all room categories");
        return roomCategoryService.getRoomCategories(pageFilter).map(mapper::toDto);

    }

    @GetMapping("/{id}")
    public ResponseEntity<RoomCategoryResponseDto> getRoomCategoryById(
            @PathVariable("id") Long id
    ) {
        log.info("Request for getting room category by id={}", id);
        var foundRoomCategory = roomCategoryService.getRoomCategoryById(id);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(mapper.toDto(foundRoomCategory));
    }

    @PostMapping("/search")
    public ResponseEntity<Page<RoomCategoryResponseDto>> getRoomCategoriesWithFilter(
            @Valid @RequestBody RoomCategorySearchRequestDto requestRoomsWithFilter,
            @Valid RoomCategoryPageFilter roomFilter
    ){
        log.info("Request for getting room categories with filter");
        var foundRoomCategoriesWithFilter = roomCategoryService
                .searchRoomCategoriesWithFilter(requestRoomsWithFilter,roomFilter);
        return ResponseEntity.
                status(HttpStatus.OK)
                .body(foundRoomCategoriesWithFilter.map(mapper::toDto));

    }

}
