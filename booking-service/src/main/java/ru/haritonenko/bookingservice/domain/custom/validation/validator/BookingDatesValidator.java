package ru.haritonenko.bookingservice.domain.custom.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;
import ru.haritonenko.bookingservice.domain.custom.validation.annotation.ValidBookingDates;
import ru.haritonenko.bookingservice.domain.db.entity.BookingEntity;

import java.util.Objects;

@Component
public class BookingDatesValidator implements ConstraintValidator<ValidBookingDates, BookingEntity> {

    @Override
    public boolean isValid(BookingEntity value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        if (Objects.isNull(value.getCheckInDate()) || Objects.isNull(value.getCheckOutDate())) {
            return true;
        }
        return value.getCheckOutDate().isAfter(value.getCheckInDate());
    }
}