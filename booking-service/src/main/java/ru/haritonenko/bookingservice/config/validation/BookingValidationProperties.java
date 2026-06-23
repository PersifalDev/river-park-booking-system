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
