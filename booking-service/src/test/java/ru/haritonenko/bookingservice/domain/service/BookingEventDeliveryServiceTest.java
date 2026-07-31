package ru.haritonenko.bookingservice.domain.service;

import org.junit.jupiter.api.Test;
import ru.haritonenko.bookingservice.config.workmode.BookingWorkModeProperties;
import ru.haritonenko.bookingservice.external.client.notification.NotificationServiceHttpClient;
import ru.haritonenko.bookingservice.external.client.payment.PaymentServiceHttpClient;
import ru.haritonenko.bookingservice.kafka.outbox.service.BookingOutboxService;
import ru.haritonenko.commonlibs.communication.WorkMode;
import ru.haritonenko.commonlibs.dto.kafka.event.BookingKafkaEvent;
import ru.haritonenko.commonlibs.dto.kafka.event.type.BookingEventType;
import ru.haritonenko.commonlibs.dto.kafka.payload.BookingKafkaPayload;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class BookingEventDeliveryServiceTest {

    private final BookingWorkModeProperties properties = new BookingWorkModeProperties();
    private final BookingOutboxService outboxService = mock(BookingOutboxService.class);
    private final PaymentServiceHttpClient paymentClient = mock(PaymentServiceHttpClient.class);
    private final NotificationServiceHttpClient notificationClient = mock(NotificationServiceHttpClient.class);
    private final BookingEventDeliveryService service =
            new BookingEventDeliveryService(properties, outboxService, paymentClient, notificationClient);
    private final BookingKafkaEvent<BookingKafkaPayload> event = bookingEvent();

    @Test
    void shouldPersistToOutboxInAsyncMode() {
        properties.setWorkMode(WorkMode.ASYNC);

        service.publish(event);

        verify(outboxService).saveEvent(event);
        verify(paymentClient, never()).handleBookingEvent(event);
        verify(notificationClient, never()).handleBookingEvent(event);
    }

    @Test
    void shouldCallDependentServicesInSyncMode() {
        properties.setWorkMode(WorkMode.SYNC);

        service.publish(event);

        verify(paymentClient).handleBookingEvent(event);
        verify(notificationClient).handleBookingEvent(event);
        verify(outboxService, never()).saveEvent(event);
    }

    private BookingKafkaEvent<BookingKafkaPayload> bookingEvent() {
        UUID bookingId = UUID.randomUUID();
        return new BookingKafkaEvent<>(
                UUID.randomUUID(),
                BookingEventType.BOOKING_HOLD_CREATED,
                "booking-service",
                bookingId.toString(),
                OffsetDateTime.now(),
                BookingKafkaPayload.builder().bookingId(bookingId).build()
        );
    }
}
