package ru.haritonenko.bookingservice.domain.custom.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import ru.haritonenko.bookingservice.domain.custom.validation.validator.AvailableRoomSearchRequestValidator;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = AvailableRoomSearchRequestValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAvailableRoomSearchRequest {

    String message() default "Available room search request is invalid";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}