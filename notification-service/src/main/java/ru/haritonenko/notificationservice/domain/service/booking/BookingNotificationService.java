package ru.haritonenko.notificationservice.domain.service.booking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.haritonenko.commonlibs.dto.kafka.event.type.NotificationEventType;
import ru.haritonenko.commonlibs.dto.kafka.payload.BookingKafkaPayload;
import ru.haritonenko.notificationservice.domain.service.NotificationService;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingNotificationService {

    private final NotificationService notificationService;

    public void sendBookingCreatedNotification(BookingKafkaPayload payload) {
        log.info("Creating BOOKING_CREATED notification: bookingId={}, userId={}", payload.bookingId(), payload.userId());
        notificationService.createNotification(payload.userId(), payload.bookingId(), null, "Заявка создана", "Заявка %s создана и передана в обработку.".formatted(code(payload)), NotificationEventType.BOOKING_CREATED);
    }

    public void sendBookingHoldCreatedNotification(BookingKafkaPayload payload) {
        log.info("Creating BOOKING_HOLD_CREATED notification: bookingId={}, userId={}", payload.bookingId(), payload.userId());
        notificationService.createNotification(payload.userId(), payload.bookingId(), null, "Бронь удержана", "Бронь %s удержана%s. Подтвердите бронирование в боте. Оплата производится при заселении у администратора.".formatted(code(payload), holdSuffix(payload)), NotificationEventType.BOOKING_HOLD_CREATED);
    }

    public void sendBookingConfirmedNotification(BookingKafkaPayload payload) {
        log.info("Creating BOOKING_CONFIRMED notification: bookingId={}, userId={}", payload.bookingId(), payload.userId());
        notificationService.createNotification(payload.userId(), payload.bookingId(), null, "Бронь подтверждена", "Бронь %s подтверждена. Оплата производится в день заезда у администратора отеля River Park.".formatted(code(payload)), NotificationEventType.BOOKING_CONFIRMED);
    }

    public void sendBookingCancelledNotification(BookingKafkaPayload payload) {
        log.info("Creating BOOKING_CANCELLED notification: bookingId={}, userId={}, reason={}", payload.bookingId(), payload.userId(), payload.cancellationReason());
        String message = payload.cancellationReason() == null || payload.cancellationReason().isBlank()
                ? "Бронь %s была отменена.".formatted(code(payload))
                : "Бронь %s отменена. %s".formatted(code(payload), payload.cancellationReason());
        notificationService.createNotification(payload.userId(), payload.bookingId(), null, "Бронь отменена", message, NotificationEventType.BOOKING_CANCELLED);
    }

    public void sendBookingExpiredNotification(BookingKafkaPayload payload) {
        log.info("Creating BOOKING_EXPIRED notification: bookingId={}, userId={}", payload.bookingId(), payload.userId());
        notificationService.createNotification(payload.userId(), payload.bookingId(), null, "Удержание истекло", "Удержание по брони %s истекло. Бронь отменена автоматически.".formatted(code(payload)), NotificationEventType.BOOKING_EXPIRED);
    }

    public void sendBookingFailedNotification(BookingKafkaPayload payload) {
        log.info("Creating BOOKING_FAILED notification: bookingId={}, userId={}, reason={}", payload.bookingId(), payload.userId(), payload.cancellationReason());
        String message = payload.cancellationReason() == null || payload.cancellationReason().isBlank()
                ? "Во время создания брони %s произошла ошибка.".formatted(code(payload))
                : "Во время создания брони %s произошла ошибка. %s".formatted(code(payload), payload.cancellationReason());
        notificationService.createNotification(payload.userId(), payload.bookingId(), null, "Ошибка бронирования", message, NotificationEventType.BOOKING_FAILED);
    }

    private String code(BookingKafkaPayload payload) {
        return payload.bookingCode() == null || payload.bookingCode().isBlank()
                ? String.valueOf(payload.bookingId())
                : payload.bookingCode();
    }

    private String holdSuffix(BookingKafkaPayload payload) {
        return payload.holdExpiresAt() == null ? "" : " до " + payload.holdExpiresAt().toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }
}
