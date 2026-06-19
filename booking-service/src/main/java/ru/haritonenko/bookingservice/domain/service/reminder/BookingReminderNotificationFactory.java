package ru.haritonenko.bookingservice.domain.service.reminder;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.haritonenko.bookingservice.config.notification.BookingReminderNotificationProperties;
import ru.haritonenko.bookingservice.config.notification.BookingReminderTextProperties;
import ru.haritonenko.bookingservice.domain.db.entity.BookingEntity;
import ru.haritonenko.bookingservice.domain.notification.BookingNotificationContent;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
@RequiredArgsConstructor
public class BookingReminderNotificationFactory {

    private final BookingReminderNotificationProperties properties;

    public BookingNotificationContent holdExpiring(BookingEntity booking) {
        BookingReminderTextProperties texts = properties.getTexts();
        return new BookingNotificationContent(
                texts.getHoldExpiringTitle(),
                texts.getHoldExpiringMessage().formatted(booking.getBookingCode(), formatDateTime(booking.getHoldExpiresAt()))
        );
    }

    public BookingNotificationContent checkIn(BookingEntity booking) {
        BookingReminderTextProperties texts = properties.getTexts();
        return new BookingNotificationContent(
                texts.getCheckInTitle(),
                texts.getCheckInMessage().formatted(booking.getBookingCode(), formatDate(booking.getCheckInDate()))
        );
    }

    public ZoneId dateZone() {
        return ZoneId.of(properties.getTexts().getDateZone());
    }

    private String formatDateTime(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return properties.getTexts().getUnknownDate();
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(properties.getTexts().getDateTimePattern());
        return dateTime.atZoneSameInstant(dateZone()).format(formatter);
    }

    private String formatDate(LocalDate date) {
        if (date == null) {
            return properties.getTexts().getUnknownDate();
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(properties.getTexts().getDatePattern());
        return date.format(formatter);
    }
}
