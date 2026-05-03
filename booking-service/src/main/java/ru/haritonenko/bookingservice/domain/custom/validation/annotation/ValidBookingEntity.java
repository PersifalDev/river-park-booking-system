package ru.haritonenko.bookingservice.domain.custom.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import ru.haritonenko.bookingservice.domain.custom.validation.validator.BookingEntityValidator;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = BookingEntityValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidBookingEntity {

    String message() default "Booking entity is invalid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}