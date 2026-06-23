package ru.haritonenko.bookingservice.config.pricing;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.RoundingMode;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.booking.price-calendar")
public class BookingPriceCalendarProperties {

    private boolean fallbackToCategoryBasePrice = true;
    private int moneyScale;
    private RoundingMode roundingMode = RoundingMode.HALF_UP;
}
