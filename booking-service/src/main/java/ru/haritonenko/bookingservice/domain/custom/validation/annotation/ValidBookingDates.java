package ru.haritonenko.bookingservice.domain.custom.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import ru.haritonenko.bookingservice.domain.custom.validation.validator.BookingDatesValidator;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = BookingDatesValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidBookingDates {

    String message() default "Check out date must be after check in date";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}