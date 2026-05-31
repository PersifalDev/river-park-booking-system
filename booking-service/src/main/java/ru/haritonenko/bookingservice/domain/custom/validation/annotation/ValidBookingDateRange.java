package ru.haritonenko.bookingservice.domain.custom.validation.annotation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import ru.haritonenko.bookingservice.domain.custom.validation.validator.BookingDateRangeValidator;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = BookingDateRangeValidator.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidBookingDateRange {

    String message() default "Booking date range is invalid";

    boolean checkInRequired() default false;

    boolean checkOutRequired() default false;

    boolean pastAllowed() default true;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}