package ru.haritonenko.catalogservice.category.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.haritonenko.catalogservice.category.domain.service.RoomCategoryService;
import ru.haritonenko.commonlibs.dto.category.RoomCategoryShortResponseDto;

@Slf4j
@RestController
@RequestMapping("api/v1/internal/catalog/rooms")
@RequiredArgsConstructor
@Tag(name = "Internal Room Categories", description = "Внутреннее API для других микросервисов")
public class InternalRoomCategoryController {

    private final RoomCategoryService roomCategoryService;

    @GetMapping("/{id}")
    @Operation(summary = "Получить краткую информацию о категории номера")
    @ApiResponse(responseCode = "200", description = "Категория найдена")
    public ResponseEntity<RoomCategoryShortResponseDto> getRoomCategorySummary(
            @Parameter(description = "ID категории номера") @PathVariable("id") Long id
    ){
        log.info("Request for internal room category summary by id={}",id);
        var category = roomCategoryService.getRoomCategoryById(id);
        return ResponseEntity.ok(new RoomCategoryShortResponseDto(
                category.id(),
                category.name().name(),
                category.description(),
                category.maxGuests(),
                category.basePrice(),
                category.totalUnits()
        ));
    }
}
