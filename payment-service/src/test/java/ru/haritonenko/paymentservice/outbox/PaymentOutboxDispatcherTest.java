package ru.haritonenko.paymentservice.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import ru.haritonenko.commonlibs.dto.kafka.event.PaymentKafkaEvent;
import ru.haritonenko.commonlibs.dto.kafka.event.type.PaymentEventType;
import ru.haritonenko.commonlibs.dto.kafka.payload.PaymentKafkaPayload;
import ru.haritonenko.paymentservice.config.PaymentOutboxProperties;
import ru.haritonenko.paymentservice.kafka.producer.sender.KafkaPaymentEventSender;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SuppressWarnings("unchecked")
class PaymentOutboxDispatcherTest {

    private final PaymentOutboxRepository repository = mock(PaymentOutboxRepository.class);
    private final KafkaPaymentEventSender sender = mock(KafkaPaymentEventSender.class);
    private final ObjectMapper objectMapper = mock(ObjectMapper.class);
    private final TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
    private final PaymentOutboxProperties properties = new PaymentOutboxProperties();
    private final PaymentOutboxDispatcher dispatcher =
            new PaymentOutboxDispatcher(repository, sender, objectMapper, transactionTemplate, properties);

    @BeforeEach
    void setUp() {
        properties.setBatchSize(10);
        properties.setMaxAttempts(3);
        properties.setRetryDelay(Duration.ofSeconds(30));
        properties.setProcessingTimeout(Duration.ofSeconds(30));
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
        doAnswer(invocation -> {
            Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(mock(TransactionStatus.class));
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
        when(repository.save(any(PaymentOutboxEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(repository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(sender.sendEvent(any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    @Test
    void shouldClaimSendAndMarkEventAsSent() {
        PaymentOutboxEntity outbox = outbox(0);
        when(repository.findReadyForUpdate(any(), eq(properties.getBatchSize())))
                .thenReturn(List.of(outbox));
        when(repository.findById(outbox.getId())).thenReturn(Optional.of(outbox));
        mockRead(outbox, event());

        dispatcher.dispatch();

        verify(sender).sendEvent(any());
        assertEquals(PaymentOutboxStatus.SENT, outbox.getStatus());
        assertNotNull(outbox.getSentAt());
    }

    @Test
    void shouldScheduleRetryOnlyAfterKafkaAcknowledgementFails() {
        PaymentOutboxEntity outbox = outbox(0);
        when(repository.findById(outbox.getId())).thenReturn(Optional.of(outbox));
        mockRead(outbox, event());
        when(sender.sendEvent(any())).thenReturn(CompletableFuture.failedFuture(new IllegalStateException("Kafka down")));

        dispatcher.sendOne(outbox.getId());

        assertEquals(PaymentOutboxStatus.NEW, outbox.getStatus());
        assertEquals(1, outbox.getAttempts());
        assertNotNull(outbox.getNextAttemptAt());
    }

    @Test
    void shouldMarkInvalidPayloadAsFailedWithoutSending() throws Exception {
        PaymentOutboxEntity outbox = outbox(0);
        when(repository.findById(outbox.getId())).thenReturn(Optional.of(outbox));
        when(objectMapper.readValue(eq(outbox.getPayload()), any(TypeReference.class)))
                .thenThrow(new JsonProcessingException("bad payload") {
                });

        dispatcher.sendOne(outbox.getId());

        assertEquals(PaymentOutboxStatus.FAILED, outbox.getStatus());
        verify(sender, never()).sendEvent(any());
    }

    private void mockRead(PaymentOutboxEntity outbox, PaymentKafkaEvent<PaymentKafkaPayload> event) {
        try {
            when(objectMapper.readValue(eq(outbox.getPayload()), any(TypeReference.class))).thenReturn(event);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private PaymentOutboxEntity outbox(int attempts) {
        return PaymentOutboxEntity.builder()
                .id(UUID.randomUUID())
                .aggregateId(UUID.randomUUID())
                .eventType(PaymentEventType.PAYMENT_PENDING.name())
                .payload("{}")
                .status(PaymentOutboxStatus.NEW)
                .attempts(attempts)
                .nextAttemptAt(OffsetDateTime.now().minusSeconds(1))
                .createdAt(OffsetDateTime.now().minusSeconds(2))
                .build();
    }

    private PaymentKafkaEvent<PaymentKafkaPayload> event() {
        UUID bookingId = UUID.randomUUID();
        UUID paymentId = UUID.randomUUID();
        return PaymentKafkaEvent.<PaymentKafkaPayload>builder()
                .eventId(UUID.randomUUID())
                .eventType(PaymentEventType.PAYMENT_PENDING)
                .source("payment-service-test")
                .correlationId(bookingId.toString())
                .createdAt(OffsetDateTime.now())
                .payload(PaymentKafkaPayload.builder()
                        .bookingId(bookingId)
                        .paymentId(paymentId)
                        .build())
                .build();
    }
}
