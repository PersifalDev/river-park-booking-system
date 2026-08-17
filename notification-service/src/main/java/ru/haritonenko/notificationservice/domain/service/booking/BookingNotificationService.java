package ru.haritonenko.notificationservice.domain.service.booking;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.haritonenko.commonlibs.dto.kafka.event.type.NotificationEventType;
import ru.haritonenko.commonlibs.dto.kafka.payload.BookingPayload;
import ru.haritonenko.notificationservice.config.BookingNotificationTextProperties;
import ru.haritonenko.notificationservice.config.NotificationTextTemplate;
import ru.haritonenko.notificationservice.domain.service.NotificationService;

import java.time.format.DateTimeFormatter;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingNotificationService {

    private final NotificationService notificationService;
    private final BookingNotificationTextProperties textProperties;

    public void sendBookingCreatedNotification(BookingPayload payload) {
        log.info("Creating BOOKING_CREATED notification: bookingId={}, userId={}", payload.bookingId(), payload.userId());
        createBookingNotification(payload, textProperties.getCreated(), NotificationEventType.BOOKING_CREATED, code(payload));
    }

    public void sendBookingHoldCreatedNotification(BookingPayload payload) {
        log.info("Creating BOOKING_HOLD_CREATED notification: bookingId={}, userId={}", payload.bookingId(), payload.userId());
        createBookingNotification(payload, textProperties.getHoldCreated(), NotificationEventType.BOOKING_HOLD_CREATED, code(payload), holdSuffix(payload));
    }

    public void sendBookingConfirmedNotification(BookingPayload payload) {
        log.info("Creating BOOKING_CONFIRMED notification: bookingId={}, userId={}", payload.bookingId(), payload.userId());
        createBookingNotification(payload, textProperties.getConfirmed(), NotificationEventType.BOOKING_CONFIRMED, code(payload));
    }

    public void sendBookingCancelledNotification(BookingPayload payload) {
        log.info("Creating BOOKING_CANCELLED notification: bookingId={}, userId={}, reason={}", payload.bookingId(), payload.userId(), payload.cancellationReason());
        createBookingNotification(payload, textProperties.getCancelled(), NotificationEventType.BOOKING_CANCELLED, code(payload));
    }

    public void sendBookingExpiredNotification(BookingPayload payload) {
        log.info("Creating BOOKING_EXPIRED notification: bookingId={}, userId={}", payload.bookingId(), payload.userId());
        createBookingNotification(payload, textProperties.getExpired(), NotificationEventType.BOOKING_EXPIRED, code(payload));
    }

    public void sendBookingFailedNotification(BookingPayload payload) {
        log.info("Creating BOOKING_FAILED notification: bookingId={}, userId={}, reason={}", payload.bookingId(), payload.userId(), payload.cancellationReason());
        String message = payload.cancellationReason() == null || payload.cancellationReason().isBlank()
                ? textProperties.getFailed().getMessage().formatted(code(payload))
                : textProperties.getFailed().getMessageWithReason().formatted(code(payload), payload.cancellationReason());
        notificationService.createNotification(payload.userId(), payload.bookingId(), null, textProperties.getFailed().getTitle(), message, NotificationEventType.BOOKING_FAILED);
    }

    private void createBookingNotification(BookingPayload payload, NotificationTextTemplate text, NotificationEventType eventType, Object... messageArgs) {
        notificationService.createNotification(
                payload.userId(),
                payload.bookingId(),
                null,
                text.getTitle(),
                text.getMessage().formatted(messageArgs),
                eventType
        );
    }

    private String code(BookingPayload payload) {
        return payload.bookingCode() == null || payload.bookingCode().isBlank()
                ? String.valueOf(payload.bookingId())
                : payload.bookingCode();
    }

    private String holdSuffix(BookingPayload payload) {
        if (payload.holdExpiresAt() == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(textProperties.getDateTimePattern());
        return " до " + payload.holdExpiresAt().toLocalDateTime().format(formatter);
    }
}
