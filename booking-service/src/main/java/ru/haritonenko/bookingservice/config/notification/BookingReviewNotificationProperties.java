package ru.haritonenko.bookingservice.config.notification;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "app.booking.notifications.review")
public class BookingReviewNotificationProperties {

    @NotBlank
    private String title = "Как прошло проживание?";

    @NotBlank
    private String message = "Спасибо, что выбрали River Park. Все понравилось? Оставьте отзыв на сайте отеля.";

    @NotBlank
    private String promoPrefix = "Ваш промокод на следующее заселение: ";
}
