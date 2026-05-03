package ru.haritonenko.paymentservice.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.haritonenko.commonlibs.dto.kafka.event.PaymentKafkaEvent;
import ru.haritonenko.commonlibs.dto.kafka.event.type.PaymentEventType;
import ru.haritonenko.commonlibs.dto.kafka.payload.BookingKafkaPayload;
import ru.haritonenko.commonlibs.dto.kafka.payload.PaymentKafkaPayload;
import ru.haritonenko.commonlibs.utils.pages.CommonPageable;
import ru.haritonenko.paymentservice.api.dto.filter.PaymentPageFilter;
import ru.haritonenko.paymentservice.domain.Payment;
import ru.haritonenko.paymentservice.domain.db.entity.PaymentEntity;
import ru.haritonenko.paymentservice.domain.db.repository.PaymentEntityRepository;
import ru.haritonenko.paymentservice.domain.exception.IllegalPaymentStateException;
import ru.haritonenko.paymentservice.domain.exception.PaymentNotFoundException;
import ru.haritonenko.paymentservice.domain.mapper.PaymentMapper;
import ru.haritonenko.paymentservice.domain.status.PaymentStatus;
import ru.haritonenko.paymentservice.kafka.producer.sender.KafkaPaymentEventSender;

import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentEntityRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final KafkaPaymentEventSender kafkaPaymentEventSender;

    @Value("${app.payment.default-page-number}")
    private int defaultPageNumber;

    @Value("${app.payment.default-page-size}")
    private int defaultPageSize;

    @Value("${app.payment.events.source}")
    private String sourceService;

    @Value("${app.payment.contact-phone}")
    private String contactPhone;

    @Value("${app.payment.default-comment}")
    private String defaultComment;

    @Value("${app.payment.default-instruction}")
    private String defaultInstruction;

    @Transactional
    public Payment createPendingPayment(BookingKafkaPayload payload) {
        if (payload == null || payload.bookingId() == null || payload.userId() == null) {
            log.warn("Skip creating payment because booking payload is invalid: payload={}", payload);
            throw new IllegalArgumentException("Booking payload is invalid");
        }
        log.info("Creating pending payment for bookingId={}, bookingCode={}, userId={}", payload.bookingId(), payload.bookingCode(), payload.userId());
        PaymentEntity existingPayment = paymentRepository.findByBookingId(payload.bookingId()).orElse(null);
        if (existingPayment != null) {
            existingPayment.setBookingCode(payload.bookingCode() == null || payload.bookingCode().isBlank() ? payload.bookingId().toString() : payload.bookingCode());
            existingPayment.setUserId(payload.userId());
            existingPayment.setPriceAmount(payload.priceAmount());
            existingPayment.setStatus(PaymentStatus.PENDING);
            existingPayment.setCancellationReason(null);
            existingPayment.setPaymentMethod("PAY_ON_ARRIVAL");
            existingPayment.setPaymentComment(defaultComment);
            existingPayment.setContactPhone(contactPhone);
            existingPayment.setPaymentInstruction(defaultInstruction);
            PaymentEntity refreshedPayment = paymentRepository.save(existingPayment);
            log.info("Pending payment refreshed for bookingId={}, paymentId={}, amount={}", payload.bookingId(), refreshedPayment.getId(), refreshedPayment.getPriceAmount());
            return paymentMapper.toDomain(refreshedPayment);
        }
        PaymentEntity savedPayment = paymentRepository.save(PaymentEntity.builder()
                .bookingId(payload.bookingId())
                .bookingCode(payload.bookingCode() == null || payload.bookingCode().isBlank() ? payload.bookingId().toString() : payload.bookingCode())
                .userId(payload.userId())
                .priceAmount(payload.priceAmount())
                .status(PaymentStatus.PENDING)
                .paymentMethod("PAY_ON_ARRIVAL")
                .paymentComment(defaultComment)
                .contactPhone(contactPhone)
                .paymentInstruction(defaultInstruction)
                .build());
        log.info("Pending payment created successfully: paymentId={}, bookingId={}", savedPayment.getId(), savedPayment.getBookingId());
        sendPaymentEvent(savedPayment, PaymentEventType.PAYMENT_PENDING);
        return paymentMapper.toDomain(savedPayment);
    }

    @Transactional(readOnly = true)
    public Payment getPaymentByBookingIdAndUserId(UUID bookingId, Long userId) {
        log.info("Getting payment by bookingId={} and userId={}", bookingId, userId);
        PaymentEntity paymentEntity = findByBookingId(bookingId);
        if (!paymentEntity.getUserId().equals(userId)) {
            log.warn("Payment does not belong to user: bookingId={}, requestedUserId={}, ownerUserId={}", bookingId, userId, paymentEntity.getUserId());
            throw new PaymentNotFoundException("Payment not found for bookingId=%s".formatted(bookingId));
        }
        return paymentMapper.toDomain(paymentEntity);
    }

    @Transactional(readOnly = true)
    public Page<Payment> getAllPaymentsByUserId(Long userId, PaymentPageFilter pageFilter) {
        log.info("Getting all payments for userId={}", userId);
        Pageable pageable = CommonPageable.getPageable(pageFilter, defaultPageNumber, defaultPageSize);
        return paymentRepository.findAllByUserId(userId, pageable).map(paymentMapper::toDomain);
    }

    @Transactional
    public Payment confirmPaymentByBookingIdAndUserId(UUID bookingId, Long userId) {
        log.info("Confirming payment by bookingId={} and userId={}", bookingId, userId);
        PaymentEntity paymentEntity = findByBookingId(bookingId);
        if (!paymentEntity.getUserId().equals(userId)) {
            log.warn("User tried to confirm чужой payment: bookingId={}, requestedUserId={}, ownerUserId={}", bookingId, userId, paymentEntity.getUserId());
            throw new PaymentNotFoundException("Payment not found for bookingId=%s".formatted(bookingId));
        }
        if (paymentEntity.getStatus() != PaymentStatus.PENDING) {
            log.warn("Payment must be in PENDING status for confirmation: paymentId={}, status={}", paymentEntity.getId(), paymentEntity.getStatus());
            throw new IllegalPaymentStateException("Payment must be in PENDING status for confirmation");
        }
        paymentEntity.setStatus(PaymentStatus.CONFIRMED);
        paymentEntity.setCancellationReason(null);
        PaymentEntity savedPayment = paymentRepository.save(paymentEntity);
        log.info("Payment confirmed successfully: paymentId={}, bookingId={}", savedPayment.getId(), savedPayment.getBookingId());
        sendPaymentEvent(savedPayment, PaymentEventType.PAYMENT_CONFIRMED);
        return paymentMapper.toDomain(savedPayment);
    }

    @Transactional
    public Payment cancelPaymentByBookingIdAndUserId(UUID bookingId, Long userId) {
        log.info("Cancelling payment by bookingId={} and userId={}", bookingId, userId);
        PaymentEntity paymentEntity = findByBookingId(bookingId);
        if (!paymentEntity.getUserId().equals(userId)) {
            log.warn("User tried to cancel чужой payment: bookingId={}, requestedUserId={}, ownerUserId={}", bookingId, userId, paymentEntity.getUserId());
            throw new PaymentNotFoundException("Payment not found for bookingId=%s".formatted(bookingId));
        }
        if (paymentEntity.getStatus() == PaymentStatus.CANCELLED || paymentEntity.getStatus() == PaymentStatus.FAILED) {
            log.warn("Payment already inactive: paymentId={}, status={}", paymentEntity.getId(), paymentEntity.getStatus());
            throw new IllegalPaymentStateException("Payment already inactive");
        }
        paymentEntity.setStatus(PaymentStatus.CANCELLED);
        paymentEntity.setCancellationReason("Cancelled by user=%s".formatted(userId));
        PaymentEntity savedPayment = paymentRepository.save(paymentEntity);
        log.info("Payment cancelled successfully: paymentId={}, bookingId={}", savedPayment.getId(), savedPayment.getBookingId());
        sendPaymentEvent(savedPayment, PaymentEventType.PAYMENT_CANCELLED);
        return paymentMapper.toDomain(savedPayment);
    }

    @Transactional
    public void cancelPaymentInternal(UUID bookingId, String cancellationReason) {
        if (bookingId == null) {
            log.warn("Skip internal payment cancellation because bookingId is null");
            return;
        }
        log.info("Cancelling payment internally for bookingId={}, reason={}", bookingId, cancellationReason);
        PaymentEntity paymentEntity = paymentRepository.findByBookingId(bookingId).orElse(null);
        if (paymentEntity == null) {
            log.info("No payment found for bookingId={} during internal cancellation", bookingId);
            return;
        }
        if (paymentEntity.getStatus() == PaymentStatus.CANCELLED || paymentEntity.getStatus() == PaymentStatus.FAILED) {
            log.info("Payment already inactive during internal cancellation: paymentId={}, status={}", paymentEntity.getId(), paymentEntity.getStatus());
            return;
        }
        paymentEntity.setStatus(PaymentStatus.CANCELLED);
        paymentEntity.setCancellationReason(cancellationReason == null || cancellationReason.isBlank() ? "Cancelled by booking lifecycle" : cancellationReason);
        PaymentEntity savedPayment = paymentRepository.save(paymentEntity);
        log.info("Internal cancellation completed for paymentId={}, bookingId={}", savedPayment.getId(), bookingId);
        sendPaymentEvent(savedPayment, PaymentEventType.PAYMENT_CANCELLED);
    }

    private PaymentEntity findByBookingId(UUID bookingId) {
        return paymentRepository.findByBookingId(bookingId).orElseThrow(() -> new PaymentNotFoundException("Payment not found for bookingId=%s".formatted(bookingId)));
    }

    private void sendPaymentEvent(PaymentEntity paymentEntity, PaymentEventType paymentEventType) {
        log.info("Preparing payment Kafka event: paymentId={}, bookingId={}, eventType={}", paymentEntity.getId(), paymentEntity.getBookingId(), paymentEventType);
        kafkaPaymentEventSender.sendEvent(new PaymentKafkaEvent<>(
                UUID.randomUUID(),
                paymentEventType,
                sourceService,
                paymentEntity.getBookingId().toString(),
                OffsetDateTime.now(),
                PaymentKafkaPayload.builder()
                        .bookingId(paymentEntity.getBookingId())
                        .bookingCode(paymentEntity.getBookingCode())
                        .paymentId(paymentEntity.getId())
                        .userId(paymentEntity.getUserId())
                        .priceAmount(paymentEntity.getPriceAmount())
                        .paymentStatus(paymentEntity.getStatus().name())
                        .paymentMethod(paymentEntity.getPaymentMethod())
                        .paymentComment(paymentEntity.getPaymentComment())
                        .contactPhone(paymentEntity.getContactPhone())
                        .paymentInstruction(paymentEntity.getPaymentInstruction())
                        .cancellationReason(paymentEntity.getCancellationReason())
                        .createdAt(paymentEntity.getCreatedAt())
                        .build()
        ));
        log.info("Payment Kafka event prepared and sent: paymentId={}, eventType={}", paymentEntity.getId(), paymentEventType);
    }
}
