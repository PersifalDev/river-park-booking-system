package ru.haritonenko.catalogservice.category.api.controller.web_only;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.haritonenko.catalogservice.category.api.controller.RoomCategoryController;
import ru.haritonenko.catalogservice.category.api.dto.RoomCategoryResponseDto;
import ru.haritonenko.catalogservice.category.api.dto.filter.RoomCategoryPageFilter;
import ru.haritonenko.catalogservice.category.api.dto.filter.RoomCategorySearchRequestDto;
import ru.haritonenko.catalogservice.category.domain.RoomCategory;
import ru.haritonenko.catalogservice.category.domain.mapper.RoomCategoryToDtoMapper;
import ru.haritonenko.catalogservice.category.domain.service.RoomCategoryService;
import ru.haritonenko.catalogservice.category.domain.type.RoomType;
import ru.haritonenko.catalogservice.security.jwt.manager.JwtTokenManager;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = RoomCategoryController.class)
@AutoConfigureMockMvc(addFilters = false)
class RoomCategoryControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoomCategoryService roomCategoryService;

    @MockitoBean
    private RoomCategoryToDtoMapper mapper;

    @MockitoBean
    private JwtTokenManager jwtTokenManager;

    private RoomCategory domain;
    private RoomCategoryResponseDto dto;

    @BeforeEach
    void setUp() {
        domain = roomCategory();
        dto = roomCategoryDto();
        when(mapper.toDto(domain)).thenReturn(dto);
    }

    @Test
    void shouldGetAllRoomCategories() throws Exception {
        when(roomCategoryService.getRoomCategories(any(RoomCategoryPageFilter.class)))
                .thenReturn(new PageImpl<>(List.of(domain)));

        mockMvc.perform(get("/api/v1/catalog/rooms")
                        .param("pageNumber", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].name").value("STANDARD"));

        verify(roomCategoryService).getRoomCategories(any(RoomCategoryPageFilter.class));
    }

    @Test
    void shouldGetRoomCategoryById() throws Exception {
        when(roomCategoryService.getRoomCategoryById(1L)).thenReturn(domain);

        mockMvc.perform(get("/api/v1/catalog/rooms/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("STANDARD"));

        verify(roomCategoryService).getRoomCategoryById(1L);
    }

    @Test
    void shouldSearchRoomCategories() throws Exception {
        when(roomCategoryService.searchRoomCategoriesWithFilter(
                any(RoomCategorySearchRequestDto.class),
                any(RoomCategoryPageFilter.class)
        )).thenReturn(new PageImpl<>(List.of(domain)));

        mockMvc.perform(post("/api/v1/catalog/rooms/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "roomType": "STANDARD",
                                  "guests": 2,
                                  "priceFrom": 3000,
                                  "priceTo": 7000
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));

        verify(roomCategoryService).searchRoomCategoriesWithFilter(
                any(RoomCategorySearchRequestDto.class),
                any(RoomCategoryPageFilter.class)
        );
    }

    private RoomCategory roomCategory() {
        return RoomCategory.builder()
                .id(1L)
                .name(RoomType.STANDARD)
                .description("Standard room")
                .maxGuests(2)
                .basePrice(BigDecimal.valueOf(5000))
                .areaSquare(20.0)
                .totalUnits(10)
                .build();
    }

    private RoomCategoryResponseDto roomCategoryDto() {
        return RoomCategoryResponseDto.builder()
                .id(1L)
                .name(RoomType.STANDARD)
                .description("Standard room")
                .maxGuests(2)
                .basePrice(BigDecimal.valueOf(5000))
                .areaSquare(20.0)
                .totalUnits(10)
                .build();
    }
}
