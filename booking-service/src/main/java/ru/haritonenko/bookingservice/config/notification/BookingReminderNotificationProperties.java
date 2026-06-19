package ru.haritonenko.bookingservice.config.notification;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.booking.reminders")
public class BookingReminderNotificationProperties {

    private Duration holdExpiringWindow = Duration.ofMinutes(5);
    private BookingReminderCheckInProperties checkIn = new BookingReminderCheckInProperties();
    private BookingReminderTextProperties texts = new BookingReminderTextProperties();
}
