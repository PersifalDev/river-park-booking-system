package ru.haritonenko.bookingservice.domain.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.haritonenko.bookingservice.api.dto.BookingRequestDto;
import ru.haritonenko.bookingservice.external.client.catalog.CatalogServiceHttpClient;
import ru.haritonenko.commonlibs.dto.category.RoomCategoryResponseDto;
import ru.haritonenko.commonlibs.dto.category.type.RoomType;
import ru.haritonenko.commonlibs.exception.BookingGuestsOverloadedException;
import ru.haritonenko.commonlibs.exception.CategoryIllegalArgumentException;
import ru.haritonenko.commonlibs.exception.RoomCategoryNotFoundException;
import ru.haritonenko.commonlibs.exception.UserIllegalArgumentException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingValidationServiceTest {

    @Mock
    private CatalogServiceHttpClient catalogServiceHttpClient;

    @Mock
    private PromoCodeService promoCodeService;

    @InjectMocks
    private BookingValidationService bookingValidationService;

    @Test
    void shouldValidateCorrectRequest() {
        when(catalogServiceHttpClient.getRoomCategoryById(1L)).thenReturn(room(2));

        assertDoesNotThrow(() -> bookingValidationService.validateBookingRequest(request(1L, 2, null), 10L));
    }

    @Test
    void shouldRejectNullCategoryId() {
        assertThrows(
                CategoryIllegalArgumentException.class,
                () -> bookingValidationService.validateBookingRequest(request(null, 2, null), 10L)
        );
    }

    @Test
    void shouldRejectNullUserId() {
        assertThrows(
                UserIllegalArgumentException.class,
                () -> bookingValidationService.validateBookingRequest(request(1L, 2, null), null)
        );
    }

    @Test
    void shouldRejectMissingCategory() {
        when(catalogServiceHttpClient.getRoomCategoryById(1L)).thenReturn(null);

        assertThrows(
                RoomCategoryNotFoundException.class,
                () -> bookingValidationService.validateBookingRequest(request(1L, 2, null), 10L)
        );
    }

    @Test
    void shouldRejectTooManyGuests() {
        when(catalogServiceHttpClient.getRoomCategoryById(1L)).thenReturn(room(2));

        assertThrows(
                BookingGuestsOverloadedException.class,
                () -> bookingValidationService.validateBookingRequest(request(1L, 3, null), 10L)
        );
    }

    private BookingRequestDto request(Long categoryId, int guests, String promoCode) {
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        return BookingRequestDto.builder()
                .categoryId(categoryId)
                .checkInDate(checkInDate)
                .checkOutDate(checkInDate.plusDays(1))
                .guests(guests)
                .adultCount(guests)
                .childrenCount(0)
                .promoCode(promoCode)
                .build();
    }

    private RoomCategoryResponseDto room(int maxGuests) {
        return new RoomCategoryResponseDto(
                1L,
                RoomType.STANDARD,
                "Standard room",
                maxGuests,
                BigDecimal.valueOf(5000),
                20.0,
                30,
                null,
                null
        );
    }
}
