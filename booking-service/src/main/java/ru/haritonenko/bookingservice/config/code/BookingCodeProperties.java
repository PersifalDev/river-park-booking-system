package ru.haritonenko.bookingservice.config.code;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.booking.code")
public class BookingCodeProperties {

    private int randomSuffixLength;
}
