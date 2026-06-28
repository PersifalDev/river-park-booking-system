package ru.haritonenko.bookingservice.domain.custom.validation.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import ru.haritonenko.bookingservice.api.dto.AvailableRoomSearchRequestDto;
import ru.haritonenko.bookingservice.config.validation.BookingValidationProperties;
import ru.haritonenko.bookingservice.domain.custom.validation.annotation.ValidAvailableRoomSearchRequest;

import java.math.BigDecimal;
import java.util.Objects;

@Component
public class AvailableRoomSearchRequestValidator
        implements ConstraintValidator<ValidAvailableRoomSearchRequest, AvailableRoomSearchRequestDto> {

    private final BookingValidationProperties properties;

    public AvailableRoomSearchRequestValidator() {
        this(BookingValidationProperties.defaults());
    }

    @Autowired
    public AvailableRoomSearchRequestValidator(BookingValidationProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean isValid(AvailableRoomSearchRequestDto value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        boolean valid = true;
        context.disableDefaultConstraintViolation();

        valid &= validatePrice("priceFrom", value.priceFrom(), context);
        valid &= validatePrice("priceTo", value.priceTo(), context);
        valid &= validateArea("minArea", value.minArea(), context);

        if (Objects.nonNull(value.priceFrom())
                && Objects.nonNull(value.priceTo())
                && value.priceFrom().compareTo(value.priceTo()) > 0) {
            context.buildConstraintViolationWithTemplate("Low bound of price can not be greater than high bound of price")
                    .addPropertyNode("priceFrom")
                    .addConstraintViolation();
            valid = false;
        }

        return valid;
    }

    private boolean validatePrice(String propertyName, BigDecimal value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        boolean valid = true;
        BookingValidationProperties.Price priceLimits = properties.getPrice();

        if (value.compareTo(priceLimits.getMin()) < 0) {
            context.buildConstraintViolationWithTemplate(propertyName + " can not be negative")
                    .addPropertyNode(propertyName)
                    .addConstraintViolation();
            valid = false;
        }

        if (value.compareTo(priceLimits.getMax()) > 0) {
            context.buildConstraintViolationWithTemplate(propertyName + " is too large")
                    .addPropertyNode(propertyName)
                    .addConstraintViolation();
            valid = false;
        }

        if (fractionDigits(value) > priceLimits.getFractionDigits()) {
            context.buildConstraintViolationWithTemplate(
                            propertyName + " must have up to " + priceLimits.getFractionDigits() + " fraction digits")
                    .addPropertyNode(propertyName)
                    .addConstraintViolation();
            valid = false;
        }

        return valid;
    }

    private boolean validateArea(String propertyName, BigDecimal value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }

        boolean valid = true;
        BookingValidationProperties.Area areaLimits = properties.getArea();

        if (value.compareTo(areaLimits.getMin()) <= 0) {
            context.buildConstraintViolationWithTemplate("Square must be positive")
                    .addPropertyNode(propertyName)
                    .addConstraintViolation();
            valid = false;
        }

        if (value.compareTo(areaLimits.getMax()) > 0) {
            context.buildConstraintViolationWithTemplate("Square is too large")
                    .addPropertyNode(propertyName)
                    .addConstraintViolation();
            valid = false;
        }

        if (fractionDigits(value) > areaLimits.getFractionDigits()) {
            context.buildConstraintViolationWithTemplate(
                            "Square must have up to " + areaLimits.getFractionDigits() + " fraction digits")
                    .addPropertyNode(propertyName)
                    .addConstraintViolation();
            valid = false;
        }

        return valid;
    }

    private int fractionDigits(BigDecimal value) {
        return Math.max(value.stripTrailingZeros().scale(), 0);
    }
}
