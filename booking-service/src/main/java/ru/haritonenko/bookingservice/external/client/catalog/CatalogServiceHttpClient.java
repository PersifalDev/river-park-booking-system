package ru.haritonenko.bookingservice.external.client.catalog;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;
import ru.haritonenko.commonlibs.dto.category.RoomCategoryResponseDto;
import ru.haritonenko.commonlibs.dto.category.RoomCategorySearchRequestDto;
import ru.haritonenko.commonlibs.utils.pages.PageResponse;

@HttpExchange(accept = "application/json", contentType = "application/json")
public interface CatalogServiceHttpClient {

    @GetExchange("/api/v1/internal/catalog/rooms/{id}")
    RoomCategoryResponseDto getRoomCategoryById(@PathVariable Long id);

    @PostExchange("/api/v1/catalog/rooms/search")
    PageResponse<RoomCategoryResponseDto> searchRoomCategories(
            @RequestBody RoomCategorySearchRequestDto request,
            @RequestParam("pageNumber") int pageNumber,
            @RequestParam("pageSize") int pageSize
    );

    @GetExchange("/api/v1/catalog/rooms")
    PageResponse<RoomCategoryResponseDto> getRoomCategories(
            @RequestParam("pageNumber") int pageNumber,
            @RequestParam("pageSize") int pageSize
    );
}
