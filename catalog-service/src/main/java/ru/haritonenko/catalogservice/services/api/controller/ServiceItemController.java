package ru.haritonenko.catalogservice.services.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.haritonenko.catalogservice.services.api.dto.ServiceItemResponseDto;
import ru.haritonenko.catalogservice.services.api.dto.filter.ServiceItemPageFilter;
import ru.haritonenko.catalogservice.services.domain.mapper.ServiceItemToDtoMapper;
import ru.haritonenko.catalogservice.services.domain.service.ServiceItemService;
import ru.haritonenko.catalogservice.services.domain.type.ServiceItemType;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/v1/catalog/services")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Hotel Services", description = "Дополнительные услуги отеля")
public class ServiceItemController {

    private final ServiceItemService service;
    private final ServiceItemToDtoMapper mapper;

    @GetMapping
    @Operation(summary = "Получить активные услуги")
    @ApiResponse(responseCode = "200", description = "Список активных услуг")
    public ResponseEntity<List<ServiceItemResponseDto>> getAllActiveServices(
            @Valid ServiceItemPageFilter pageFilter
    ) {
        log.info("Request to get all active services");
        var response = service.getAllActiveServicesWithPageable(pageFilter)
                .stream()
                .map(mapper::toDto)
                .toList();

        return ResponseEntity
                .status(HttpStatus.OK)
                .body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Получить активную услугу по id")
    @ApiResponse(responseCode = "200", description = "Услуга найдена")
    public ResponseEntity<ServiceItemResponseDto> getActiveServiceById(
            @Parameter(description = "ID услуги") @PathVariable("id") Long id
    ) {
        log.info("Request to get active service by id={}", id);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(mapper.toDto(service.getActiveServiceById(id)));
    }

    @GetMapping("/by-type/{type}")
    @Operation(summary = "Получить активную услугу по типу")
    @ApiResponse(responseCode = "200", description = "Услуга найдена")
    public ResponseEntity<ServiceItemResponseDto> getActiveServiceByType(
            @Parameter(description = "Тип услуги") @PathVariable ServiceItemType type
    ) {
        log.info("Request to get active service by type={}", type);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(mapper.toDto(service.getActiveServiceByType(type)));
    }
}
