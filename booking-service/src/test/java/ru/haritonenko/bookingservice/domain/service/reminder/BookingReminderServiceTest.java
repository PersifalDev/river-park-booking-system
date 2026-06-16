package ru.haritonenko.bookingservice.domain.service.reminder;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ru.haritonenko.bookingservice.domain.db.entity.BookingEntity;
import ru.haritonenko.bookingservice.domain.service.BookingService;
import ru.haritonenko.commonlibs.dto.kafka.event.type.NotificationEventType;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingReminderServiceTest {

    private final BookingService bookingService = mock(BookingService.class);

    private final BookingReminderService service = new BookingReminderService(bookingService);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "holdExpiringWindow", Duration.ofMinutes(5));
        ReflectionTestUtils.setField(service, "checkInDaysBefore", 1L);
    }

    @Test
    void shouldSendHoldExpiringReminderAndMarkSent() {
        BookingEntity booking = booking();
        booking.setHoldExpiresAt(OffsetDateTime.now().plusMinutes(3));
        when(bookingService.findHoldBookingsForReminder(any(), any())).thenReturn(List.of(booking));

        service.sendHoldExpiringReminders();

        verify(bookingService).sendDirectNotification(
                eq(booking),
                eq(NotificationEventType.BOOKING_HOLD_EXPIRING),
                any(),
                org.mockito.ArgumentMatchers.contains(booking.getBookingCode())
        );
        verify(bookingService).markHoldReminderSent(eq(booking.getId()), any());
    }

    @Test
    void shouldNotSendHoldReminderWhenNoBookings() {
        when(bookingService.findHoldBookingsForReminder(any(), any())).thenReturn(List.of());

        service.sendHoldExpiringReminders();

        verify(bookingService, never()).sendDirectNotification(any(), any(), any(), any());
    }

    @Test
    void shouldSendCheckInReminderAndMarkSent() {
        BookingEntity booking = booking();
        booking.setCheckInDate(LocalDate.now().plusDays(1));
        when(bookingService.findBookingsForCheckInReminder(any())).thenReturn(List.of(booking));

        service.sendCheckInReminders();

        verify(bookingService).sendDirectNotification(
                eq(booking),
                eq(NotificationEventType.BOOKING_CHECK_IN_REMINDER),
                any(),
                org.mockito.ArgumentMatchers.contains(booking.getBookingCode())
        );
        verify(bookingService).markCheckInReminderSent(eq(booking.getId()), any());
    }

    private BookingEntity booking() {
        return BookingEntity.builder()
                .id(UUID.randomUUID())
                .bookingCode("BK-TEST")
                .build();
    }
}
