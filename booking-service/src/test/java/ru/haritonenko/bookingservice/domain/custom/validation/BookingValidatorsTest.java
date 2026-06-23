package ru.haritonenko.bookingservice.domain.custom.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorFactory;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import ru.haritonenko.bookingservice.api.dto.AvailableRoomSearchRequestDto;
import ru.haritonenko.bookingservice.api.dto.BookingRequestDto;
import ru.haritonenko.bookingservice.config.validation.BookingValidationProperties;
import ru.haritonenko.bookingservice.domain.custom.validation.validator.AvailableRoomSearchRequestValidator;
import ru.haritonenko.bookingservice.domain.custom.validation.validator.BookingDateRangeValidator;
import ru.haritonenko.bookingservice.domain.custom.validation.validator.GuestCountsValidator;
import ru.haritonenko.commonlibs.dto.category.type.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BookingValidatorsTest {

    private final BookingValidationProperties properties = validationProperties();
    private final Validator validator = Validation.byDefaultProvider()
            .configure()
            .constraintValidatorFactory(new TestConstraintValidatorFactory(properties))
            .buildValidatorFactory()
            .getValidator();

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
                2,
                0,
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

    private BookingValidationProperties validationProperties() {
        BookingValidationProperties properties = new BookingValidationProperties();
        properties.getGuests().setMinTotal(1);
        properties.getGuests().setMaxTotal(6);
        properties.getGuests().setMinAdults(1);
        properties.getGuests().setMaxAdults(4);
        properties.getGuests().setMinChildren(0);
        properties.getGuests().setMaxChildren(5);
        properties.getPrice().setMin(BigDecimal.valueOf(0));
        properties.getPrice().setMax(BigDecimal.valueOf(1_000_000));
        properties.getPrice().setFractionDigits(2);
        properties.getArea().setMin(BigDecimal.valueOf(0));
        properties.getArea().setMax(BigDecimal.valueOf(1_000));
        properties.getArea().setFractionDigits(2);
        return properties;
    }

    private static class TestConstraintValidatorFactory implements ConstraintValidatorFactory {

        private final BookingValidationProperties properties;

        private TestConstraintValidatorFactory(BookingValidationProperties properties) {
            this.properties = properties;
        }

        @Override
        public <T extends ConstraintValidator<?, ?>> T getInstance(Class<T> key) {
            if (key == BookingDateRangeValidator.class) {
                return key.cast(new BookingDateRangeValidator(properties));
            }
            if (key == GuestCountsValidator.class) {
                return key.cast(new GuestCountsValidator(properties));
            }
            if (key == AvailableRoomSearchRequestValidator.class) {
                return key.cast(new AvailableRoomSearchRequestValidator(properties));
            }
            try {
                return key.getDeclaredConstructor().newInstance();
            } catch (ReflectiveOperationException exception) {
                throw new IllegalStateException("Can not instantiate validator " + key.getName(), exception);
            }
        }

        @Override
        public void releaseInstance(ConstraintValidator<?, ?> instance) {
        }
    }
}
