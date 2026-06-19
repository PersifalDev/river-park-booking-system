package ru.haritonenko.notificationservice.domain.service.payment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.haritonenko.commonlibs.dto.kafka.event.type.NotificationEventType;
import ru.haritonenko.notificationservice.config.NotificationTextTemplate;
import ru.haritonenko.notificationservice.config.PaymentNotificationTextProperties;
import ru.haritonenko.notificationservice.domain.service.NotificationService;

import java.math.BigDecimal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentNotificationService {

    private final NotificationService notificationService;
    private final PaymentNotificationTextProperties textProperties;

    public void sendPaymentCreatedNotification(UUID bookingId, UUID paymentId, Long userId, BigDecimal amount) {
        log.info("Creating PAYMENT_INVOICE_CREATED notification: bookingId={}, paymentId={}, userId={}", bookingId, paymentId, userId);
        createPaymentNotification(userId, bookingId, paymentId, textProperties.getInvoiceCreated(), NotificationEventType.PAYMENT_INVOICE_CREATED, formatAmount(amount));
    }

    public void sendPaymentPendingNotification(UUID bookingId, UUID paymentId, Long userId, BigDecimal amount) {
        log.info("Creating PAYMENT_PENDING notification: bookingId={}, paymentId={}, userId={}", bookingId, paymentId, userId);
        createPaymentNotification(userId, bookingId, paymentId, textProperties.getPending(), NotificationEventType.PAYMENT_PENDING, formatAmount(amount));
    }

    public void sendPaymentConfirmedNotification(UUID bookingId, UUID paymentId, Long userId) {
        log.info("Creating PAYMENT_CONFIRMED notification: bookingId={}, paymentId={}, userId={}", bookingId, paymentId, userId);
        createPaymentNotification(userId, bookingId, paymentId, textProperties.getConfirmed(), NotificationEventType.PAYMENT_CONFIRMED);
    }

    public void sendPaymentCancelledNotification(UUID bookingId, UUID paymentId, Long userId, String reason) {
        log.info("Creating PAYMENT_CANCELLED notification: bookingId={}, paymentId={}, userId={}", bookingId, paymentId, userId);
        createPaymentNotification(userId, bookingId, paymentId, textProperties.getCancelled(), NotificationEventType.PAYMENT_CANCELLED);
    }

    public void sendPaymentFailedNotification(UUID bookingId, UUID paymentId, Long userId, String reason) {
        log.info("Creating PAYMENT_FAILED notification: bookingId={}, paymentId={}, userId={}", bookingId, paymentId, userId);
        createPaymentNotification(userId, bookingId, paymentId, textProperties.getFailed(), NotificationEventType.PAYMENT_FAILED);
    }

    private void createPaymentNotification(
            Long userId,
            UUID bookingId,
            UUID paymentId,
            NotificationTextTemplate text,
            NotificationEventType type,
            Object... messageArgs
    ) {
        notificationService.createNotification(
                userId,
                bookingId,
                paymentId,
                text.getTitle(),
                text.getMessage().formatted(messageArgs),
                type
        );
    }

    private String formatAmount(BigDecimal amount) {
        return amount == null ? textProperties.getUnknownAmount() : amount.stripTrailingZeros().toPlainString();
    }
}
