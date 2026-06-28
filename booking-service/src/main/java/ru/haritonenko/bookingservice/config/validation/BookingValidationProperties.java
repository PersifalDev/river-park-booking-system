package ru.haritonenko.bookingservice.config.validation;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;
import java.time.ZoneId;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.booking.validation")
public class BookingValidationProperties {

    private ZoneId dateZone = ZoneId.of("Asia/Novosibirsk");

    private Guests guests = new Guests();

    private Price price = new Price();

    private Area area = new Area();

    public static BookingValidationProperties defaults() {
        BookingValidationProperties properties = new BookingValidationProperties();
        properties.guests.minTotal = 1;
        properties.guests.maxTotal = 6;
        properties.guests.minAdults = 1;
        properties.guests.maxAdults = 4;
        properties.guests.minChildren = 0;
        properties.guests.maxChildren = 5;
        properties.price.min = new BigDecimal("0.00");
        properties.price.max = new BigDecimal("1000000.00");
        properties.price.fractionDigits = 2;
        properties.area.min = new BigDecimal("0.00");
        properties.area.max = new BigDecimal("1000.00");
        properties.area.fractionDigits = 2;
        return properties;
    }

    @Getter
    @Setter
    public static class Guests {

        private int minTotal;

        private int maxTotal;

        private int minAdults;

        private int maxAdults;

        private int minChildren;

        private int maxChildren;
    }

    @Getter
    @Setter
    public static class Price {

        private BigDecimal min;

        private BigDecimal max;

        private int fractionDigits;
    }

    @Getter
    @Setter
    public static class Area {

        private BigDecimal min;

        private BigDecimal max;

        private int fractionDigits;
    }
}
