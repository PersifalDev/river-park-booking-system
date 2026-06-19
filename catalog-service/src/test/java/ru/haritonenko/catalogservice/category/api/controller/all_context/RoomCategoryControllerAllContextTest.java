package ru.haritonenko.catalogservice.category.api.controller.all_context;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.haritonenko.catalogservice.category.api.dto.RoomCategoryResponseDto;
import ru.haritonenko.catalogservice.category.api.dto.filter.RoomCategoryPageFilter;
import ru.haritonenko.catalogservice.category.domain.RoomCategory;
import ru.haritonenko.catalogservice.category.domain.mapper.RoomCategoryToDtoMapper;
import ru.haritonenko.catalogservice.category.domain.service.RoomCategoryService;
import ru.haritonenko.catalogservice.category.domain.type.RoomType;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:catalog-controller-context;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.jpa.database-platform=org.hibernate.dialect.H2Dialect",
        "spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.H2Dialect",
        "spring.liquibase.enabled=false"
})
@AutoConfigureMockMvc(addFilters = false)
class RoomCategoryControllerAllContextTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private RoomCategoryService roomCategoryService;

    @MockitoBean
    private RoomCategoryToDtoMapper mapper;

    @Test
    void shouldLoadApplicationContextAndGetRoomCategories() throws Exception {
        RoomCategory roomCategory = roomCategory();
        when(roomCategoryService.getRoomCategories(any(RoomCategoryPageFilter.class)))
                .thenReturn(new PageImpl<>(List.of(roomCategory)));
        when(mapper.toDto(roomCategory)).thenReturn(roomCategoryDto());

        mockMvc.perform(get("/api/v1/catalog/rooms")
                        .param("pageNumber", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("STANDARD"));
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
