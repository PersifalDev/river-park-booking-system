package ru.haritonenko.bookingservice.domain.custom.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.stereotype.Component;
import ru.haritonenko.bookingservice.domain.custom.validation.annotation.NotPastDate;

import java.time.LocalDate;

@Component
public class NotPastDateValidator implements ConstraintValidator<NotPastDate, LocalDate> {

    @Override
    public boolean isValid(LocalDate value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        return !value.isBefore(LocalDate.now());
    }
}