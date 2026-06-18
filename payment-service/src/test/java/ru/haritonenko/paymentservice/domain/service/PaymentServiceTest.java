package ru.haritonenko.paymentservice.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import ru.haritonenko.commonlibs.dto.kafka.payload.BookingKafkaPayload;
import ru.haritonenko.paymentservice.domain.Payment;
import ru.haritonenko.paymentservice.domain.db.entity.PaymentEntity;
import ru.haritonenko.paymentservice.domain.db.repository.PaymentEntityRepository;
import ru.haritonenko.paymentservice.domain.exception.IllegalPaymentStateException;
import ru.haritonenko.paymentservice.domain.exception.PaymentNotFoundException;
import ru.haritonenko.paymentservice.domain.mapper.PaymentMapper;
import ru.haritonenko.paymentservice.domain.status.PaymentStatus;
import ru.haritonenko.paymentservice.kafka.producer.sender.KafkaPaymentEventSender;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    private PaymentEntityRepository repository;
    private KafkaPaymentEventSender sender;
    private PaymentService service;

    @BeforeEach
    void setUp() {
        repository = mock(PaymentEntityRepository.class);
        sender = mock(KafkaPaymentEventSender.class);
        service = new PaymentService(repository, new PaymentMapper(), sender);
        ReflectionTestUtils.setField(service, "sourceService", "payment-service");
        ReflectionTestUtils.setField(service, "contactPhone", "+7 383 000-00-00");
        ReflectionTestUtils.setField(service, "defaultComment", "Оплата при заселении");
        ReflectionTestUtils.setField(service, "defaultInstruction", "Оплатите на стойке регистрации");
        ReflectionTestUtils.setField(service, "defaultPageNumber", 0);
        ReflectionTestUtils.setField(service, "defaultPageSize", 10);
    }

    @Test
    void shouldCreatePendingPaymentAndSendEvent() {
        UUID bookingId = UUID.randomUUID();
        when(repository.findByBookingId(bookingId)).thenReturn(Optional.empty());
        when(repository.save(any(PaymentEntity.class))).thenAnswer(invocation -> {
            PaymentEntity entity = invocation.getArgument(0);
            entity.setId(UUID.randomUUID());
            return entity;
        });

        Payment actual = service.createPendingPayment(BookingKafkaPayload.builder()
                .bookingId(bookingId)
                .bookingCode("BK-1")
                .userId(10L)
                .priceAmount(BigDecimal.valueOf(5000))
                .build());

        assertEquals(PaymentStatus.PENDING, actual.status());
        assertEquals("PAY_ON_ARRIVAL", actual.paymentMethod());
        verify(sender).sendEvent(any());
    }

    @Test
    void shouldRejectAccessToAnotherUsersPayment() {
        UUID bookingId = UUID.randomUUID();
        when(repository.findByBookingId(bookingId)).thenReturn(Optional.of(payment(bookingId, 20L, PaymentStatus.PENDING)));

        assertThrows(PaymentNotFoundException.class, () -> service.getPaymentByBookingIdAndUserId(bookingId, 10L));
    }

    @Test
    void shouldConfirmPendingPayment() {
        UUID bookingId = UUID.randomUUID();
        PaymentEntity entity = payment(bookingId, 10L, PaymentStatus.PENDING);
        when(repository.findByBookingId(bookingId)).thenReturn(Optional.of(entity));
        when(repository.save(entity)).thenReturn(entity);

        Payment actual = service.confirmPaymentByBookingIdAndUserId(bookingId, 10L);

        assertEquals(PaymentStatus.CONFIRMED, actual.status());
        verify(sender).sendEvent(any());
    }

    @Test
    void shouldRejectConfirmWhenPaymentIsNotPending() {
        UUID bookingId = UUID.randomUUID();
        when(repository.findByBookingId(bookingId)).thenReturn(Optional.of(payment(bookingId, 10L, PaymentStatus.CONFIRMED)));

        assertThrows(IllegalPaymentStateException.class, () -> service.confirmPaymentByBookingIdAndUserId(bookingId, 10L));
    }

    private PaymentEntity payment(UUID bookingId, Long userId, PaymentStatus status) {
        return PaymentEntity.builder()
                .id(UUID.randomUUID())
                .bookingId(bookingId)
                .bookingCode("BK-1")
                .userId(userId)
                .priceAmount(BigDecimal.valueOf(5000))
                .status(status)
                .paymentMethod("PAY_ON_ARRIVAL")
                .paymentComment("Оплата при заселении")
                .contactPhone("+7 383 000-00-00")
                .paymentInstruction("Оплатите на стойке регистрации")
                .build();
    }
}
