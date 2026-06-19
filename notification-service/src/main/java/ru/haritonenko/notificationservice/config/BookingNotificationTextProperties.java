package ru.haritonenko.notificationservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.notification.texts.booking")
public class BookingNotificationTextProperties {

    private String dateTimePattern;
    private NotificationTextTemplate created;
    private NotificationTextTemplate holdCreated;
    private NotificationTextTemplate confirmed;
    private NotificationTextTemplate cancelled;
    private NotificationTextTemplate expired;
    private NotificationTextTemplate failed;

    public String getDateTimePattern() {
        return dateTimePattern;
    }

    public void setDateTimePattern(String dateTimePattern) {
        this.dateTimePattern = dateTimePattern;
    }

    public NotificationTextTemplate getCreated() {
        return created;
    }

    public void setCreated(NotificationTextTemplate created) {
        this.created = created;
    }

    public NotificationTextTemplate getHoldCreated() {
        return holdCreated;
    }

    public void setHoldCreated(NotificationTextTemplate holdCreated) {
        this.holdCreated = holdCreated;
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

    public NotificationTextTemplate getExpired() {
        return expired;
    }

    public void setExpired(NotificationTextTemplate expired) {
        this.expired = expired;
    }

    public NotificationTextTemplate getFailed() {
        return failed;
    }

    public void setFailed(NotificationTextTemplate failed) {
        this.failed = failed;
    }
}
