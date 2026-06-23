package ru.haritonenko.bookingservice.config.notification;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingReminderCheckInProperties {
    private long daysBefore;
}
