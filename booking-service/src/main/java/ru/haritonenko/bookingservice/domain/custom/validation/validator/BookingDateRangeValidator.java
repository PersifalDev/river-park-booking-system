package ru.haritonenko.bookingservice.domain.custom.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.haritonenko.bookingservice.config.validation.BookingValidationProperties;
import ru.haritonenko.bookingservice.domain.custom.validation.BookingDateRangeData;
import ru.haritonenko.bookingservice.domain.custom.validation.annotation.ValidBookingDateRange;


import java.time.LocalDate;
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class BookingDateRangeValidator implements ConstraintValidator<ValidBookingDateRange, BookingDateRangeData> {

    private final BookingValidationProperties properties;

    private boolean checkInRequired;

    private boolean checkOutRequired;

    private boolean pastAllowed;

    @Override
    public void initialize(ValidBookingDateRange constraintAnnotation) {
        this.checkInRequired = constraintAnnotation.checkInRequired();
        this.checkOutRequired = constraintAnnotation.checkOutRequired();
        this.pastAllowed = constraintAnnotation.pastAllowed();
    }

    @Override
    public boolean isValid(BookingDateRangeData value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        boolean valid = true;
        context.disableDefaultConstraintViolation();

        LocalDate checkInDate = value.checkInDate();
        LocalDate checkOutDate = value.checkOutDate();
        LocalDate today = LocalDate.now(properties.getDateZone());

        if (checkInRequired && Objects.isNull(checkInDate)) {
            context.buildConstraintViolationWithTemplate("Check in date can not be null")
                    .addPropertyNode("checkInDate")
                    .addConstraintViolation();
            valid = false;
        }

        if (checkOutRequired && Objects.isNull(checkOutDate)) {
            context.buildConstraintViolationWithTemplate("Check out date can not be null")
                    .addPropertyNode("checkOutDate")
                    .addConstraintViolation();
            valid = false;
        }

        if (!pastAllowed && Objects.nonNull(checkInDate) && checkInDate.isBefore(today)) {
            context.buildConstraintViolationWithTemplate("Check in date can not be in the past")
                    .addPropertyNode("checkInDate")
                    .addConstraintViolation();
            valid = false;
        }

        if (!pastAllowed && Objects.nonNull(checkOutDate) && checkOutDate.isBefore(today)) {
            context.buildConstraintViolationWithTemplate("Check out date can not be in the past")
                    .addPropertyNode("checkOutDate")
                    .addConstraintViolation();
            valid = false;
        }

        if (Objects.nonNull(checkInDate)
                && Objects.nonNull(checkOutDate)
                && !checkOutDate.isAfter(checkInDate)) {
            context.buildConstraintViolationWithTemplate("Check out date must be after check in date")
                    .addPropertyNode("checkOutDate")
                    .addConstraintViolation();
            valid = false;
        }

        return valid;
    }
}