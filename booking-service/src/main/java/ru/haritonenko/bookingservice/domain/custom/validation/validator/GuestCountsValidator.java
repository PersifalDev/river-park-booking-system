package ru.haritonenko.bookingservice.domain.custom.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.haritonenko.bookingservice.config.validation.BookingValidationProperties;
import ru.haritonenko.bookingservice.domain.custom.validation.GuestCountData;
import ru.haritonenko.bookingservice.domain.custom.validation.annotation.ValidGuestCounts;
import java.util.Objects;

@Component
public class GuestCountsValidator implements ConstraintValidator<ValidGuestCounts, GuestCountData> {

    private final BookingValidationProperties properties;

    private boolean guestsRequired;

    private boolean adultCountRequired;

    private boolean childrenCountRequired;

    private boolean compositionRequired;

    public GuestCountsValidator() {
        this(new BookingValidationProperties());
    }

    @Autowired
    public GuestCountsValidator(BookingValidationProperties properties) {
        this.properties = properties;
    }

    @Override
    public void initialize(ValidGuestCounts constraintAnnotation) {
        this.guestsRequired = constraintAnnotation.guestsRequired();
        this.adultCountRequired = constraintAnnotation.adultCountRequired();
        this.childrenCountRequired = constraintAnnotation.childrenCountRequired();
        this.compositionRequired = constraintAnnotation.compositionRequired();
    }

    @Override
    public boolean isValid(GuestCountData value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        boolean valid = true;
        context.disableDefaultConstraintViolation();

        Integer guests = value.guests();
        Integer adultCount = value.adultCount();
        Integer childrenCount = value.childrenCount();

        BookingValidationProperties.Guests limits = properties.getGuests();

        if (guestsRequired && Objects.isNull(guests)) {
            context.buildConstraintViolationWithTemplate("Guests count can not be null")
                    .addPropertyNode("guests")
                    .addConstraintViolation();
            valid = false;
        }

        if (adultCountRequired && Objects.isNull(adultCount)) {
            context.buildConstraintViolationWithTemplate("Adult count can not be null")
                    .addPropertyNode("adultCount")
                    .addConstraintViolation();
            valid = false;
        }

        if (childrenCountRequired && Objects.isNull(childrenCount)) {
            context.buildConstraintViolationWithTemplate("Children count can not be null")
                    .addPropertyNode("childrenCount")
                    .addConstraintViolation();
            valid = false;
        }

        if (Objects.nonNull(guests)) {
            if (guests < limits.getMinTotal()) {
                context.buildConstraintViolationWithTemplate(
                                "Guests count must be greater than or equal to " + limits.getMinTotal())
                        .addPropertyNode("guests")
                        .addConstraintViolation();
                valid = false;
            }

            if (guests > limits.getMaxTotal()) {
                context.buildConstraintViolationWithTemplate(
                                "Guests count can not be greater than " + limits.getMaxTotal())
                        .addPropertyNode("guests")
                        .addConstraintViolation();
                valid = false;
            }
        }

        if (Objects.nonNull(adultCount)) {
            if (adultCount < limits.getMinAdults()) {
                context.buildConstraintViolationWithTemplate(
                                "Adult count must be greater than or equal to " + limits.getMinAdults())
                        .addPropertyNode("adultCount")
                        .addConstraintViolation();
                valid = false;
            }

            if (adultCount > limits.getMaxAdults()) {
                context.buildConstraintViolationWithTemplate(
                                "Adult count can not be greater than " + limits.getMaxAdults())
                        .addPropertyNode("adultCount")
                        .addConstraintViolation();
                valid = false;
            }
        }

        if (Objects.nonNull(childrenCount)) {
            if (childrenCount < limits.getMinChildren()) {
                context.buildConstraintViolationWithTemplate(
                                "Children count must be greater than or equal to " + limits.getMinChildren())
                        .addPropertyNode("childrenCount")
                        .addConstraintViolation();
                valid = false;
            }

            if (childrenCount > limits.getMaxChildren()) {
                context.buildConstraintViolationWithTemplate(
                                "Children count can not be greater than " + limits.getMaxChildren())
                        .addPropertyNode("childrenCount")
                        .addConstraintViolation();
                valid = false;
            }
        }

        if (Objects.nonNull(adultCount)
                && Objects.nonNull(childrenCount)
                && (long) adultCount + childrenCount > limits.getMaxTotal()) {
            context.buildConstraintViolationWithTemplate(
                            "Total guests count can not be greater than " + limits.getMaxTotal())
                    .addPropertyNode("guests")
                    .addConstraintViolation();
            valid = false;
        }

        if (compositionRequired
                && Objects.nonNull(guests)
                && Objects.nonNull(adultCount)
                && Objects.nonNull(childrenCount)
                && !Objects.equals(guests, adultCount + childrenCount)) {
            context.buildConstraintViolationWithTemplate("Guests count must be equal to adult count plus children count")
                    .addPropertyNode("guests")
                    .addConstraintViolation();
            valid = false;
        }

        return valid;
    }
}
