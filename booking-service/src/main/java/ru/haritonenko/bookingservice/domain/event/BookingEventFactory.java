package ru.haritonenko.bookingservice.domain.event;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.haritonenko.bookingservice.domain.db.entity.BookingEntity;
import ru.haritonenko.commonlibs.dto.kafka.event.BookingKafkaEvent;
import ru.haritonenko.commonlibs.dto.kafka.event.NotificationKafkaEvent;
import ru.haritonenko.commonlibs.dto.kafka.event.type.BookingEventType;
import ru.haritonenko.commonlibs.dto.kafka.event.type.NotificationEventType;
import ru.haritonenko.commonlibs.dto.kafka.payload.BookingKafkaPayload;
import ru.haritonenko.commonlibs.dto.kafka.payload.NotificationKafkaPayload;
import ru.haritonenko.commonlibs.notification.NotificationStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
public class BookingEventFactory {

    @Value("${app.booking.events.source}")
    private String sourceService;

    public BookingKafkaEvent<BookingKafkaPayload> bookingEvent(BookingEntity booking, BookingEventType type) {
        return BookingKafkaEvent.<BookingKafkaPayload>builder()
                .eventId(UUID.randomUUID())
                .correlationId(booking.getId().toString())
                .source(sourceService)
                .eventType(type)
                .createdAt(OffsetDateTime.now())
                .payload(BookingKafkaPayload.builder()
                        .bookingId(booking.getId())
                        .bookingCode(booking.getBookingCode())
                        .userId(booking.getUserId())
                        .roomCategoryId(booking.getRoomCategoryId())
                        .guests(booking.getGuests())
                        .adultCount(booking.getAdultCount())
                        .childrenCount(booking.getChildrenCount())
                        .checkInDate(booking.getCheckInDate())
                        .checkOutDate(booking.getCheckOutDate())
                        .priceAmount(booking.getPriceAmount())
                        .bookingStatus(booking.getStatus().name())
                        .holdExpiresAt(booking.getHoldExpiresAt())
                        .cancellationReason(booking.getCancellationReason())
                        .build())
                .build();
    }

    public NotificationKafkaEvent<NotificationKafkaPayload> notificationEvent(
            BookingEntity booking,
            NotificationEventType type,
            String title,
            String message
    ) {
        UUID notificationId = UUID.randomUUID();
        OffsetDateTime now = OffsetDateTime.now();
        return new NotificationKafkaEvent<>(
                UUID.randomUUID(),
                type,
                sourceService,
                booking.getId().toString(),
                now,
                new NotificationKafkaPayload(
                        notificationId,
                        booking.getUserId(),
                        booking.getId(),
                        null,
                        title,
                        message,
                        type,
                        NotificationStatus.NEW,
                        now
                )
        );
    }
}
