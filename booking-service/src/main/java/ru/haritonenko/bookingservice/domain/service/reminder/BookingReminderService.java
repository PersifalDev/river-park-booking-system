package ru.haritonenko.bookingservice.domain.service.reminder;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.haritonenko.bookingservice.domain.db.entity.BookingEntity;
import ru.haritonenko.bookingservice.domain.service.BookingService;
import ru.haritonenko.commonlibs.dto.kafka.event.type.NotificationEventType;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingReminderService {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final BookingService bookingService;

    @Value("${app.booking.reminders.hold-expiring-window:5m}")
    private Duration holdExpiringWindow;

    @Value("${app.booking.reminders.check-in.days-before:1}")
    private long checkInDaysBefore;

    @Scheduled(fixedDelayString = "${app.booking.reminders.poll-delay-ms:60000}")
    public void sendHoldExpiringReminders() {
        OffsetDateTime now = OffsetDateTime.now();
        OffsetDateTime to = now.plus(holdExpiringWindow);
        List<BookingEntity> bookings = bookingService.findHoldBookingsForReminder(now, to);
        if (bookings.isEmpty()) {
            return;
        }

        for (BookingEntity booking : bookings) {
            bookingService.sendDirectNotification(
                    booking,
                    NotificationEventType.BOOKING_HOLD_EXPIRING,
                    "Удержание скоро истечёт",
                    "Бронь %s удерживается до %s. Подтвердите её в боте, иначе удержание будет снято автоматически."
                            .formatted(booking.getBookingCode(), formatDateTime(booking.getHoldExpiresAt()))
            );
            bookingService.markHoldReminderSent(booking.getId(), now);
        }
    }

    @Scheduled(fixedDelayString = "${app.booking.reminders.poll-delay-ms:60000}")
    public void sendCheckInReminders() {
        LocalDate targetDate = LocalDate.now().plusDays(checkInDaysBefore);
        List<BookingEntity> bookings = bookingService.findBookingsForCheckInReminder(targetDate);
        if (bookings.isEmpty()) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        for (BookingEntity booking : bookings) {
            bookingService.sendDirectNotification(
                    booking,
                    NotificationEventType.BOOKING_CHECK_IN_REMINDER,
                    "Напоминание о заезде",
                    "Напоминаем о брони %s. Заезд запланирован на %s. Оплата производится при заселении у администратора River Park."
                            .formatted(booking.getBookingCode(), formatDate(booking.getCheckInDate()))
            );
            bookingService.markCheckInReminderSent(booking.getId(), now);
        }
    }

    private String formatDateTime(OffsetDateTime dateTime) {
        return dateTime == null ? "—" : dateTime.toLocalDateTime().format(DATE_TIME_FORMATTER);
    }

    private String formatDate(LocalDate date) {
        return date == null ? "—" : date.format(DATE_FORMATTER);
    }
}
