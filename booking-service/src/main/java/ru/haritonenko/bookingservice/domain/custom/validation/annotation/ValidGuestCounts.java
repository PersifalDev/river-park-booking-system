package ru.haritonenko.bookingservice.domain.custom.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import ru.haritonenko.bookingservice.domain.custom.validation.validator.GuestCountsValidator;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = GuestCountsValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidGuestCounts {

    String message() default "Guest counts are invalid";

    boolean guestsRequired() default false;

    boolean adultCountRequired() default false;

    boolean childrenCountRequired() default false;

    boolean validateComposition() default false;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}