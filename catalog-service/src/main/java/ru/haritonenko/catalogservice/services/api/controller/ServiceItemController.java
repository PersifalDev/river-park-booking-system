package ru.haritonenko.catalogservice.services.api.controller;

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
public class ServiceItemController {

    private final ServiceItemService service;
    private final ServiceItemToDtoMapper mapper;

    @GetMapping
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
    public ResponseEntity<ServiceItemResponseDto> getActiveServiceById(
            @PathVariable("id") Long id
    ) {
        log.info("Request to get active service by id={}", id);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(mapper.toDto(service.getActiveServiceById(id)));
    }

    @GetMapping("/by-type/{type}")
    public ResponseEntity<ServiceItemResponseDto> getActiveServiceByType(
            @PathVariable ServiceItemType type
    ) {
        log.info("Request to get active service by type={}", type);
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(mapper.toDto(service.getActiveServiceByType(type)));
    }
}