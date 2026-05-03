package ru.haritonenko.bookingservice.domain.custom.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import ru.haritonenko.bookingservice.domain.custom.validation.validator.BookingRequestValidator;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = BookingRequestValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidBookingRequest {

    String message() default "Booking request is invalid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
