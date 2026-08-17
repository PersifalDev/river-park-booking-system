package ru.haritonenko.paymentservice.domain.service;

import org.junit.jupiter.api.Test;
import ru.haritonenko.commonlibs.communication.WorkMode;
import ru.haritonenko.commonlibs.dto.kafka.event.PaymentEvent;
import ru.haritonenko.commonlibs.dto.kafka.event.type.PaymentEventType;
import ru.haritonenko.commonlibs.dto.kafka.payload.PaymentPayload;
import ru.haritonenko.paymentservice.config.PaymentWorkModeProperties;
import ru.haritonenko.paymentservice.external.BookingServiceHttpClient;
import ru.haritonenko.paymentservice.external.NotificationServiceHttpClient;
import ru.haritonenko.paymentservice.outbox.PaymentOutboxService;

import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class PaymentEventDeliveryServiceTest {

    private final PaymentWorkModeProperties properties = new PaymentWorkModeProperties();
    private final PaymentOutboxService outboxService = mock(PaymentOutboxService.class);
    private final BookingServiceHttpClient bookingClient = mock(BookingServiceHttpClient.class);
    private final NotificationServiceHttpClient notificationClient = mock(NotificationServiceHttpClient.class);
    private final PaymentEventDeliveryService service =
            new PaymentEventDeliveryService(properties, outboxService, bookingClient, notificationClient);
    private final PaymentEvent<PaymentPayload> event = paymentEvent();

    @Test
    void shouldPersistToOutboxInAsyncMode() {
        properties.setWorkMode(WorkMode.ASYNC);

        service.publish(event);

        verify(outboxService).save(event);
        verify(bookingClient, never()).handlePaymentEvent(event);
        verify(notificationClient, never()).handlePaymentEvent(event);
    }

    @Test
    void shouldCallDependentServicesInSyncMode() {
        properties.setWorkMode(WorkMode.SYNC);

        service.publish(event);

        verify(bookingClient).handlePaymentEvent(event);
        verify(notificationClient).handlePaymentEvent(event);
        verify(outboxService, never()).save(event);
    }

    private PaymentEvent<PaymentPayload> paymentEvent() {
        UUID bookingId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        return new PaymentEvent<>(
                UUID.randomUUID(),
                PaymentEventType.PAYMENT_PENDING,
                "payment-service",
                bookingId.toString(),
                OffsetDateTime.now(),
                PaymentPayload.builder()
                        .bookingId(bookingId)
                        .paymentId(paymentId)
                        .build()
        );
    }
}
