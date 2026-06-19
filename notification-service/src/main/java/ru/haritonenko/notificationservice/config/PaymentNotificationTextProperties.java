package ru.haritonenko.notificationservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.notification.texts.payment")
public class PaymentNotificationTextProperties {

    private String unknownAmount;
    private NotificationTextTemplate invoiceCreated;
    private NotificationTextTemplate pending;
    private NotificationTextTemplate confirmed;
    private NotificationTextTemplate cancelled;
    private NotificationTextTemplate failed;

    public String getUnknownAmount() {
        return unknownAmount;
    }

    public void setUnknownAmount(String unknownAmount) {
        this.unknownAmount = unknownAmount;
    }

    public NotificationTextTemplate getInvoiceCreated() {
        return invoiceCreated;
    }

    public void setInvoiceCreated(NotificationTextTemplate invoiceCreated) {
        this.invoiceCreated = invoiceCreated;
    }

    public NotificationTextTemplate getPending() {
        return pending;
    }

    public void setPending(NotificationTextTemplate pending) {
        this.pending = pending;
    }

    public NotificationTextTemplate getConfirmed() {
        return confirmed;
    }

    public void setConfirmed(NotificationTextTemplate confirmed) {
        this.confirmed = confirmed;
    }

    public NotificationTextTemplate getCancelled() {
        return cancelled;
    }

    public void setCancelled(NotificationTextTemplate cancelled) {
        this.cancelled = cancelled;
    }

    public NotificationTextTemplate getFailed() {
        return failed;
    }

    public void setFailed(NotificationTextTemplate failed) {
        this.failed = failed;
    }
}
