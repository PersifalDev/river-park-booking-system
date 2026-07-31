package ru.haritonenko.bookingservice.lock;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@Getter
@Setter
@ConfigurationProperties(prefix = "app.booking.lock")
public class BookingLockProperties {

    private boolean enabled = true;
    private Duration waitTime;
    private Duration leaseTime;
    private Duration retrySleepTime;
    private boolean watchdogEnabled = true;
    private Duration renewInterval;
}
