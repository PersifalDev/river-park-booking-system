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

    @Getter
    @Setter
    public static class Guests {

        private int minTotal = 1;

        private int maxTotal = 6;

        private int minAdults = 1;

        private int maxAdults = 4;

        private int minChildren = 0;

        private int maxChildren = 5;
    }

    @Getter
    @Setter
    public static class Price {

        private BigDecimal min = new BigDecimal("0.00");

        private BigDecimal max = new BigDecimal("1000000.00");

        private int fractionDigits = 2;
    }

    @Getter
    @Setter
    public static class Area {

        private BigDecimal min = new BigDecimal("0.00");

        private BigDecimal max = new BigDecimal("1000.00");

        private int fractionDigits = 2;
    }
}
