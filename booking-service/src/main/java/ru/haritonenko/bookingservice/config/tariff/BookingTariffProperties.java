package ru.haritonenko.bookingservice.config.tariff;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.math.RoundingMode;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.booking.tariff")
public class BookingTariffProperties {

    @NotBlank
    private String defaultCode = "ROOM_ONLY";

    private int moneyScale = 2;

    private RoundingMode roundingMode = RoundingMode.HALF_UP;
}
