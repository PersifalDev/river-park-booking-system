package ru.haritonenko.bookingservice.config.notification;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookingReminderTextProperties {

    @NotBlank
    private String dateZone = "Asia/Novosibirsk";

    @NotBlank
    private String datePattern = "dd.MM.yyyy";

    @NotBlank
    private String dateTimePattern = "dd.MM.yyyy HH:mm";

    @NotBlank
    private String unknownDate = "-";

    @NotBlank
    private String holdExpiringTitle = "Удержание скоро истечёт";

    @NotBlank
    private String holdExpiringMessage = "Бронь %s удерживается до %s. Подтвердите бронь, иначе удержание будет снято автоматически.";

    @NotBlank
    private String checkInTitle = "Напоминание о заезде";

    @NotBlank
    private String checkInMessage = "Напоминаем о брони %s. Заезд запланирован на %s. Оплата производится при заселении у администратора River Park.";
}
