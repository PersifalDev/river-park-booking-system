package ru.haritonenko.catalogservice.services.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;
import ru.haritonenko.catalogservice.photo.service.domain.ServiceItemPhoto;
import ru.haritonenko.catalogservice.photo.service.domain.service.ServiceItemPhotoService;
import ru.haritonenko.catalogservice.services.api.dto.filter.ServiceItemPageFilter;
import ru.haritonenko.catalogservice.services.domain.ServiceItem;
import ru.haritonenko.catalogservice.services.domain.db.entity.ServiceItemEntity;
import ru.haritonenko.catalogservice.services.domain.db.repository.ServiceItemRepository;
import ru.haritonenko.catalogservice.services.domain.exception.ServiceItemNotFoundException;
import ru.haritonenko.catalogservice.services.domain.mapper.ServiceItemEntityToDomainMapper;
import ru.haritonenko.catalogservice.services.domain.type.ServiceItemType;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServiceItemServiceTest {

    private ServiceItemRepository repository;
    private ServiceItemPhotoService photoService;
    private ServiceItemService service;

    @BeforeEach
    void setUp() {
        repository = mock(ServiceItemRepository.class);
        photoService = mock(ServiceItemPhotoService.class);
        service = new ServiceItemService(repository, new ServiceItemEntityToDomainMapper(), photoService);
        ReflectionTestUtils.setField(service, "defaultPageNumber", 0);
        ReflectionTestUtils.setField(service, "defaultPageSize", 10);
    }

    @Test
    void shouldReturnActiveServicesWithPhoto() {
        ServiceItemEntity gym = entity(1L, ServiceItemType.GYM, 10, true);
        when(repository.findByIsActiveTrueOrderBySortOrderAsc(any(PageRequest.class))).thenReturn(List.of(gym));
        when(photoService.getByServiceItemId(1L)).thenReturn(ServiceItemPhoto.builder()
                .id(1L)
                .serviceItemId(1L)
                .path("data/static/services/gym.jpg")
                .build());

        List<ServiceItem> result = service.getAllActiveServicesWithPageable(new ServiceItemPageFilter());

        assertEquals(1, result.size());
        assertEquals(ServiceItemType.GYM, result.getFirst().type());
        assertEquals("data/static/services/gym.jpg", result.getFirst().photoPath());
    }

    @Test
    void shouldGetActiveServiceByType() {
        ServiceItemEntity gym = entity(1L, ServiceItemType.GYM, 10, true);
        when(repository.findByTypeAndIsActiveTrue(ServiceItemType.GYM)).thenReturn(Optional.of(gym));
        when(photoService.getByServiceItemId(1L)).thenReturn(ServiceItemPhoto.builder()
                .id(1L)
                .serviceItemId(1L)
                .path("data/static/services/gym.jpg")
                .build());

        ServiceItem result = service.getActiveServiceByType(ServiceItemType.GYM);

        assertEquals(1L, result.id());
        assertEquals(ServiceItemType.GYM, result.type());
    }

    @Test
    void shouldThrowWhenActiveServiceNotFound() {
        when(repository.findByIdAndIsActiveTrue(1L)).thenReturn(Optional.empty());

        assertThrows(ServiceItemNotFoundException.class, () -> service.getActiveServiceById(1L));
    }

    private ServiceItemEntity entity(Long id, ServiceItemType type, int sortOrder, boolean active) {
        return ServiceItemEntity.builder()
                .id(id)
                .type(type)
                .title(type.getValue())
                .description(type.getValue() + " description")
                .sortOrder(sortOrder)
                .isActive(active)
                .build();
    }
}
