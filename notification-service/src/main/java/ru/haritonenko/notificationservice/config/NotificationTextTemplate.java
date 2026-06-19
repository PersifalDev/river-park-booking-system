package ru.haritonenko.notificationservice.config;

public class NotificationTextTemplate {
    private String title;
    private String message;
    private String messageWithReason;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getMessageWithReason() {
        return messageWithReason;
    }

    public void setMessageWithReason(String messageWithReason) {
        this.messageWithReason = messageWithReason;
    }
}
