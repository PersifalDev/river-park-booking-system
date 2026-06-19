package ru.haritonenko.bookingservice.domain.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.haritonenko.bookingservice.config.notification.BookingReviewNotificationProperties;
import ru.haritonenko.bookingservice.domain.db.entity.BookingEntity;

@Component
@RequiredArgsConstructor
public class BookingReviewNotificationFactory {

    private final BookingReviewNotificationProperties properties;

    public BookingNotificationContent build(BookingEntity booking, String promoCode) {
        StringBuilder message = new StringBuilder(properties.getMessage());
        if (promoCode != null && !promoCode.isBlank()) {
            message.append("\n\n")
                    .append(properties.getPromoPrefix())
                    .append(promoCode);
            if (booking.getPromoDiscountPercent() != null) {
                message.append(" (-").append(booking.getPromoDiscountPercent()).append("%)");
            }
        }
        return new BookingNotificationContent(properties.getTitle(), message.toString());
    }
}
