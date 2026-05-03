package ru.haritonenko.notificationservice.domain.service.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.haritonenko.commonlibs.dto.kafka.event.type.NotificationEventType;
import ru.haritonenko.notificationservice.domain.service.NotificationService;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentNotificationService {

    private final NotificationService notificationService;

    public void sendPaymentCreatedNotification(UUID bookingId, UUID paymentId, Long userId, BigDecimal amount) {
        log.info("Creating PAYMENT_INVOICE_CREATED notification: bookingId={}, paymentId={}, userId={}", bookingId, paymentId, userId);
        notificationService.createNotification(userId, bookingId, paymentId, "Инструкция по бронированию", "Сумма проживания рассчитана: %s. Подтвердите намерение приехать. Оплата производится при заселении у администратора.".formatted(amount), NotificationEventType.PAYMENT_INVOICE_CREATED);
    }

    public void sendPaymentPendingNotification(UUID bookingId, UUID paymentId, Long userId, BigDecimal amount) {
        log.info("Creating PAYMENT_PENDING notification: bookingId={}, paymentId={}, userId={}", bookingId, paymentId, userId);
        notificationService.createNotification(userId, bookingId, paymentId, "Ожидается подтверждение", "Сумма проживания: %s. Нажмите подтверждение в боте, чтобы зафиксировать бронирование.".formatted(amount), NotificationEventType.PAYMENT_PENDING);
    }

    public void sendPaymentConfirmedNotification(UUID bookingId, UUID paymentId, Long userId) {
        log.info("Creating PAYMENT_CONFIRMED notification: bookingId={}, paymentId={}, userId={}", bookingId, paymentId, userId);
        notificationService.createNotification(userId, bookingId, paymentId, "Подтверждение получено", "Подтверждение намерения оплатить на месте получено. Ожидайте итогового подтверждения брони.", NotificationEventType.PAYMENT_CONFIRMED);
    }

    public void sendPaymentCancelledNotification(UUID bookingId, UUID paymentId, Long userId, String reason) {
        log.info("Creating PAYMENT_CANCELLED notification: bookingId={}, paymentId={}, userId={}", bookingId, paymentId, userId);
        notificationService.createNotification(userId, bookingId, paymentId, "Подтверждение отменено", reason == null || reason.isBlank() ? "Подтверждение оплаты на месте было отменено." : reason, NotificationEventType.PAYMENT_CANCELLED);
    }

    public void sendPaymentFailedNotification(UUID bookingId, UUID paymentId, Long userId, String reason) {
        log.info("Creating PAYMENT_FAILED notification: bookingId={}, paymentId={}, userId={}", bookingId, paymentId, userId);
        notificationService.createNotification(userId, bookingId, paymentId, "Ошибка подтверждения", reason == null || reason.isBlank() ? "Во время обработки подтверждения возникла ошибка." : reason, NotificationEventType.PAYMENT_FAILED);
    }
}
