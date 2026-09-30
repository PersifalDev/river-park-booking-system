package ru.haritonenko.bookingservice.domain.event;

import org.junit.jupiter.api.Test;
import ru.haritonenko.bookingservice.domain.db.entity.BookingEntity;
import ru.haritonenko.bookingservice.domain.status.BookingStatus;
import ru.haritonenko.commonlibs.dto.kafka.event.type.BookingEventType;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class BookingEventFactoryTest {
    @Test
    void retriedHoldShouldKeepEventIdForRecipientDeduplication() {
        var factory = new BookingEventFactory();
        var booking = BookingEntity.builder().id(UUID.randomUUID()).status(BookingStatus.HOLD).build();
        assertEquals(factory.bookingEvent(booking, BookingEventType.BOOKING_HOLD_CREATED).eventId(),
                factory.bookingEvent(booking, BookingEventType.BOOKING_HOLD_CREATED).eventId());
        var other = booking.toBuilder().id(UUID.randomUUID()).build();
        assertNotEquals(factory.bookingEvent(booking, BookingEventType.BOOKING_HOLD_CREATED).eventId(),
                factory.bookingEvent(other, BookingEventType.BOOKING_HOLD_CREATED).eventId());
    }
}
