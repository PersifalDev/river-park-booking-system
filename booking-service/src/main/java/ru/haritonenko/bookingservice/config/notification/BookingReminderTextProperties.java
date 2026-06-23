package ru.haritonenko.bookingservice.config.notification;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingReminderTextProperties {

    @NotBlank
    private String dateZone;

    @NotBlank
    private String datePattern;

    @NotBlank
    private String dateTimePattern;

    @NotBlank
    private String unknownDate;

    @NotBlank
    private String holdExpiringTitle;

    @NotBlank
    private String holdExpiringMessage;

    @NotBlank
    private String checkInTitle;

    @NotBlank
    private String checkInMessage;
}
