package ru.haritonenko.bookingservice.domain.custom.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;
import ru.haritonenko.bookingservice.api.dto.BookingRequestDto;
import ru.haritonenko.bookingservice.domain.custom.validation.annotation.ValidBookingRequest;

import java.util.Objects;

@Component
public class BookingRequestValidator implements ConstraintValidator<ValidBookingRequest, BookingRequestDto> {

    @Override
    public boolean isValid(BookingRequestDto value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        boolean valid = true;
        context.disableDefaultConstraintViolation();

        if (Objects.nonNull(value.checkInDate())
                && Objects.nonNull(value.checkOutDate())
                && !value.checkOutDate().isAfter(value.checkInDate())) {
            context.buildConstraintViolationWithTemplate("Check out date must be after check in date")
                    .addPropertyNode("checkOutDate")
                    .addConstraintViolation();
            valid = false;
        }

        if (Objects.nonNull(value.guests())
                && Objects.nonNull(value.adultCount())
                && Objects.nonNull(value.childrenCount())
                && !Objects.equals(value.guests(), value.adultCount() + value.childrenCount())) {
            context.buildConstraintViolationWithTemplate("Guests count must be equal to adult count plus children count")
                    .addPropertyNode("guests")
                    .addConstraintViolation();
            valid = false;
        }

        return valid;
    }
}