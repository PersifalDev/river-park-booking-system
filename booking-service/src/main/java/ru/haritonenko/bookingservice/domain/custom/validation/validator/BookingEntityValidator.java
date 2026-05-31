package ru.haritonenko.bookingservice.domain.custom.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;
import ru.haritonenko.bookingservice.domain.custom.validation.annotation.ValidBookingEntity;
import ru.haritonenko.bookingservice.domain.db.entity.BookingEntity;
import ru.haritonenko.bookingservice.domain.status.BookingStatus;

import java.util.Objects;

@Component
public class BookingEntityValidator implements ConstraintValidator<ValidBookingEntity, BookingEntity> {

    @Override
    public boolean isValid(BookingEntity value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        boolean valid = true;
        context.disableDefaultConstraintViolation();

        boolean activeHoldStatus = BookingStatus.CREATED.equals(value.getStatus())
                || BookingStatus.HOLD.equals(value.getStatus());

        if (activeHoldStatus && Objects.isNull(value.getHoldExpiresAt())) {
            context.buildConstraintViolationWithTemplate("Hold expiration moment is required for CREATED and HOLD statuses")
                    .addPropertyNode("holdExpiresAt")
                    .addConstraintViolation();
            valid = false;
        }

        if (!activeHoldStatus && Objects.nonNull(value.getHoldExpiresAt())) {
            context.buildConstraintViolationWithTemplate("Hold expiration moment must be null for inactive or confirmed booking statuses")
                    .addPropertyNode("holdExpiresAt")
                    .addConstraintViolation();
            valid = false;
        }

        return valid;
    }
}
