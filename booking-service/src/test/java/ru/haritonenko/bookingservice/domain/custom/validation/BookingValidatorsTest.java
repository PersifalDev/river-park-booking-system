package ru.haritonenko.bookingservice.domain.custom.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import ru.haritonenko.bookingservice.api.dto.AvailableRoomSearchRequestDto;
import ru.haritonenko.bookingservice.api.dto.BookingRequestDto;
import ru.haritonenko.commonlibs.dto.category.type.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingValidatorsTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void shouldAcceptValidBookingRequest() {
        assertTrue(validator.validate(bookingRequest(2, 2, 0)).isEmpty());
    }

    @Test
    void shouldRejectGuestsCompositionMismatch() {
        Set<ConstraintViolation<BookingRequestDto>> violations = validator.validate(bookingRequest(3, 2, 0));

        assertFalse(violations.isEmpty());
        assertTrue(violations.stream().anyMatch(v -> v.getPropertyPath().toString().contains("guests")));
    }

    @Test
    void shouldRejectInvalidDateRange() {
        LocalDate checkInDate = LocalDate.now().plusDays(2);
        BookingRequestDto request = BookingRequestDto.builder()
                .categoryId(1L)
                .checkInDate(checkInDate)
                .checkOutDate(checkInDate)
                .guests(2)
                .adultCount(2)
                .childrenCount(0)
                .build();

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldRejectAvailableRoomSearchWhenPriceFromGreaterThanPriceTo() {
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        AvailableRoomSearchRequestDto request = new AvailableRoomSearchRequestDto(
                checkInDate,
                checkInDate.plusDays(1),
                2,
                RoomType.STANDARD,
                BigDecimal.valueOf(9000),
                BigDecimal.valueOf(5000),
                BigDecimal.valueOf(20)
        );

        assertFalse(validator.validate(request).isEmpty());
    }

    @Test
    void shouldAllowMostlyOptionalAvailableRoomSearch() {
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        AvailableRoomSearchRequestDto request = new AvailableRoomSearchRequestDto(
                checkInDate,
                checkInDate.plusDays(1),
                null,
                null,
                null,
                null,
                null
        );

        assertTrue(validator.validate(request).isEmpty());
    }

    private BookingRequestDto bookingRequest(int guests, int adults, int children) {
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        return BookingRequestDto.builder()
                .categoryId(1L)
                .checkInDate(checkInDate)
                .checkOutDate(checkInDate.plusDays(1))
                .guests(guests)
                .adultCount(adults)
                .childrenCount(children)
                .build();
    }
}
