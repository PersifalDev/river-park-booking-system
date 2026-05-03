package ru.haritonenko.bookingservice.domain.custom.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import ru.haritonenko.bookingservice.domain.custom.validation.validator.NotPastDateValidator;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = NotPastDateValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface NotPastDate {

    String message() default "Date must be today or in the future";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}