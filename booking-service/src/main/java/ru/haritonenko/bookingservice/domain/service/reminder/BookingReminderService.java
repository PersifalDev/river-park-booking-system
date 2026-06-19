package ru.haritonenko.bookingservice.domain.service.reminder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.haritonenko.bookingservice.config.notification.BookingReminderNotificationProperties;
import ru.haritonenko.bookingservice.domain.db.entity.BookingEntity;
import ru.haritonenko.bookingservice.domain.notification.BookingNotificationContent;
import ru.haritonenko.bookingservice.domain.service.BookingService;
import ru.haritonenko.commonlibs.dto.kafka.event.type.NotificationEventType;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingReminderService {

    private final BookingService bookingService;
    private final BookingReminderNotificationProperties properties;
    private final BookingReminderNotificationFactory notificationFactory;

    @Scheduled(fixedDelayString = "${app.booking.reminders.poll-delay-ms:60000}")
    public void sendHoldExpiringReminders() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime to = now.plus(properties.getHoldExpiringWindow());
        List<BookingEntity> bookings = bookingService.findHoldBookingsForReminder(now, to);
        if (bookings.isEmpty()) {
            return;
        }

        for (BookingEntity booking : bookings) {
            BookingNotificationContent notification = notificationFactory.holdExpiring(booking);
            bookingService.sendDirectNotification(
                    booking,
                    NotificationEventType.BOOKING_HOLD_EXPIRING,
                    notification.title(),
                    notification.message()
            );
            bookingService.markHoldReminderSent(booking.getId(), now);
        }
    }

    @Scheduled(fixedDelayString = "${app.booking.reminders.poll-delay-ms:60000}")
    public void sendCheckInReminders() {
        LocalDate targetDate = LocalDate.now(notificationFactory.dateZone()).plusDays(properties.getCheckIn().getDaysBefore());
        List<BookingEntity> bookings = bookingService.findBookingsForCheckInReminder(targetDate);
        if (bookings.isEmpty()) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        for (BookingEntity booking : bookings) {
            BookingNotificationContent notification = notificationFactory.checkIn(booking);
            bookingService.sendDirectNotification(
                    booking,
                    NotificationEventType.BOOKING_CHECK_IN_REMINDER,
                    notification.title(),
                    notification.message()
            );
            bookingService.markCheckInReminderSent(booking.getId(), now);
        }
    }
}
