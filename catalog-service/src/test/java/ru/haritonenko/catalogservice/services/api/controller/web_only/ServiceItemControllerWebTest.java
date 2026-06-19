package ru.haritonenko.catalogservice.services.api.controller.web_only;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.haritonenko.catalogservice.services.api.controller.ServiceItemController;
import ru.haritonenko.catalogservice.services.api.dto.ServiceItemResponseDto;
import ru.haritonenko.catalogservice.services.api.dto.filter.ServiceItemPageFilter;
import ru.haritonenko.catalogservice.services.domain.ServiceItem;
import ru.haritonenko.catalogservice.services.domain.mapper.ServiceItemToDtoMapper;
import ru.haritonenko.catalogservice.services.domain.service.ServiceItemService;
import ru.haritonenko.catalogservice.services.domain.type.ServiceItemType;
import ru.haritonenko.catalogservice.security.jwt.manager.JwtTokenManager;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ServiceItemController.class)
@AutoConfigureMockMvc(addFilters = false)
class ServiceItemControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ServiceItemService serviceItemService;

    @MockitoBean
    private ServiceItemToDtoMapper mapper;

    @MockitoBean
    private JwtTokenManager jwtTokenManager;

    private ServiceItem serviceItem;
    private ServiceItemResponseDto dto;

    @BeforeEach
    void setUp() {
        serviceItem = ServiceItem.builder()
                .id(1L)
                .type(ServiceItemType.GYM)
                .title("Gym")
                .description("Training room")
                .isActive(true)
                .sortOrder(10)
                .build();
        dto = ServiceItemResponseDto.builder()
                .id(1L)
                .type("GYM")
                .title("Gym")
                .description("Training room")
                .build();
        when(mapper.toDto(serviceItem)).thenReturn(dto);
    }

    @Test
    void shouldGetAllActiveServices() throws Exception {
        when(serviceItemService.getAllActiveServicesWithPageable(any(ServiceItemPageFilter.class)))
                .thenReturn(List.of(serviceItem));

        mockMvc.perform(get("/api/v1/catalog/services")
                        .param("pageNumber", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].type").value("GYM"));

        verify(serviceItemService).getAllActiveServicesWithPageable(any(ServiceItemPageFilter.class));
    }

    @Test
    void shouldGetActiveServiceById() throws Exception {
        when(serviceItemService.getActiveServiceById(1L)).thenReturn(serviceItem);

        mockMvc.perform(get("/api/v1/catalog/services/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Gym"));

        verify(serviceItemService).getActiveServiceById(1L);
    }

    @Test
    void shouldGetActiveServiceByType() throws Exception {
        when(serviceItemService.getActiveServiceByType(ServiceItemType.GYM)).thenReturn(serviceItem);

        mockMvc.perform(get("/api/v1/catalog/services/by-type/{type}", "GYM"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("GYM"));

        verify(serviceItemService).getActiveServiceByType(ServiceItemType.GYM);
    }
}
