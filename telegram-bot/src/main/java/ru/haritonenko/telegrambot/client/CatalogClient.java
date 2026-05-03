package ru.haritonenko.telegrambot.client;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.haritonenko.commonlibs.dto.category.RoomCategoryResponseDto;
import ru.haritonenko.commonlibs.dto.category.RoomCategorySearchRequestDto;
import ru.haritonenko.commonlibs.dto.photo.RoomCategoryPhotoResponseDto;
import ru.haritonenko.commonlibs.dto.rule.RuleDocumentResponseDto;
import ru.haritonenko.commonlibs.dto.service.ServiceItemResponseDto;
import ru.haritonenko.commonlibs.utils.pages.PageResponse;


import java.util.List;

@Component
@RequiredArgsConstructor
public class CatalogClient {

    private static final ParameterizedTypeReference<PageResponse<RoomCategoryResponseDto>> ROOM_PAGE_TYPE =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<PageResponse<RoomCategoryPhotoResponseDto>> PHOTO_PAGE_TYPE =
            new ParameterizedTypeReference<>() {};
    private static final ParameterizedTypeReference<List<ServiceItemResponseDto>> SERVICE_LIST_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient catalogRestClient;

    public PageResponse<RoomCategoryResponseDto> getRooms(int pageNumber, int pageSize) {
        var response = catalogRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/catalog/rooms")
                        .queryParam("pageNumber", pageNumber)
                        .queryParam("pageSize", pageSize)
                        .build())
                .retrieve()
                .body(ROOM_PAGE_TYPE);
        return response == null ? new PageResponse<>(List.of(), 0, 0, pageSize, pageNumber) : response;
    }

    public RoomCategoryResponseDto getRoomById(Long id) {
        return catalogRestClient.get()
                .uri("/api/v1/catalog/rooms/{id}", id)
                .retrieve()
                .body(RoomCategoryResponseDto.class);
    }

    public PageResponse<RoomCategoryResponseDto> searchRooms(RoomCategorySearchRequestDto request, int pageNumber, int pageSize) {
        var response = catalogRestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/catalog/rooms/search")
                        .queryParam("pageNumber", pageNumber)
                        .queryParam("pageSize", pageSize)
                        .build())
                .body(request)
                .retrieve()
                .body(ROOM_PAGE_TYPE);
        return response == null ? new PageResponse<>(List.of(), 0, 0, pageSize, pageNumber) : response;
    }

    public List<RoomCategoryPhotoResponseDto> getRoomPhotos(Long categoryId, int pageNumber, int pageSize) {
        var response = catalogRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/catalog/rooms/{categoryId}/photos")
                        .queryParam("pageNumber", pageNumber)
                        .queryParam("pageSize", pageSize)
                        .build(categoryId))
                .retrieve()
                .body(PHOTO_PAGE_TYPE);
        return response == null || response.content() == null ? List.of() : response.content();
    }

    public List<ServiceItemResponseDto> getServices(int pageNumber, int pageSize) {
        var response = catalogRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/catalog/services")
                        .queryParam("pageNumber", pageNumber)
                        .queryParam("pageSize", pageSize)
                        .build())
                .retrieve()
                .body(SERVICE_LIST_TYPE);
        return response == null ? List.of() : response;
    }

    public ServiceItemResponseDto getServiceById(Long id) {
        return catalogRestClient.get()
                .uri("/api/v1/catalog/services/{id}", id)
                .retrieve()
                .body(ServiceItemResponseDto.class);
    }

    public ServiceItemResponseDto getServiceByType(String type) {
        return catalogRestClient.get()
                .uri("/api/v1/catalog/services/by-type/{type}", type)
                .retrieve()
                .body(ServiceItemResponseDto.class);
    }

    public RuleDocumentResponseDto getRuleDocument() {
        return catalogRestClient.get()
                .uri("/api/v1/catalog/rules/document")
                .retrieve()
                .body(RuleDocumentResponseDto.class);
    }

    public byte[] downloadRuleDocument() {
        return catalogRestClient.get()
                .uri("/api/v1/catalog/rules/document/file")
                .retrieve()
                .body(byte[].class);
    }
}
