package ru.haritonenko.bookingservice.config.promo;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.RoundingMode;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.booking.promo")
public class BookingPromoProperties {

    private int generatedDiscountPercent;
    private int maxGenerationAttempts;
    private int generatedCodeRandomLength;
    private int percentDenominator;
    private int discountCalculationScale;
    private int moneyScale;
    private RoundingMode roundingMode;
}
