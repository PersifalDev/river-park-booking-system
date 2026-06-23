package ru.haritonenko.bookingservice.config.cancellation;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.booking.cancellation")
public class BookingCancellationProperties {

    @NotBlank
    private String userReason;
}
