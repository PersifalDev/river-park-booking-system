package ru.haritonenko.paymentservice.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.haritonenko.commonlibs.security.authorization.user.AuthUser;
import ru.haritonenko.paymentservice.api.dto.filter.PaymentPageFilter;
import ru.haritonenko.paymentservice.api.dto.PaymentResponseDto;
import ru.haritonenko.paymentservice.domain.mapper.PaymentMapper;
import ru.haritonenko.paymentservice.domain.service.PaymentService;
import ru.haritonenko.paymentservice.security.service.AuthenticationService;

import java.util.UUID;

@Slf4j
@Validated
@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;
    private final AuthenticationService authenticationService;

    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<PaymentResponseDto> getPaymentByBookingId(@PathVariable UUID bookingId) {
        Long authUserId = getAuthenticatedUser().id();
        log.info("Request for getting payment by bookingId={} for userId={}", bookingId, authUserId);
        return ResponseEntity.ok(paymentMapper.toDto(paymentService.getPaymentByBookingIdAndUserId(bookingId, authUserId)));
    }

    @GetMapping
    public ResponseEntity<Page<PaymentResponseDto>> getAllPayments(@ModelAttribute PaymentPageFilter pageFilter) {
        Long authUserId = getAuthenticatedUser().id();
        log.info("Request for getting all payments by userId={}", authUserId);
        return ResponseEntity.ok(paymentService.getAllPaymentsByUserId(authUserId, pageFilter).map(paymentMapper::toDto));
    }

    @PatchMapping("/booking/{bookingId}/confirm")
    public ResponseEntity<PaymentResponseDto> confirmPayment(@PathVariable UUID bookingId) {
        Long authUserId = getAuthenticatedUser().id();
        log.info("Request for confirming payment by bookingId={} for userId={}", bookingId, authUserId);
        return ResponseEntity.ok(paymentMapper.toDto(paymentService.confirmPaymentByBookingIdAndUserId(bookingId, authUserId)));
    }

    @PatchMapping("/booking/{bookingId}/cancel")
    public ResponseEntity<PaymentResponseDto> cancelPayment(@PathVariable UUID bookingId) {
        Long authUserId = getAuthenticatedUser().id();
        log.info("Request for cancelling payment by bookingId={} for userId={}", bookingId, authUserId);
        return ResponseEntity.ok(paymentMapper.toDto(paymentService.cancelPaymentByBookingIdAndUserId(bookingId, authUserId)));
    }

    private AuthUser getAuthenticatedUser() {
        AuthUser authUser = authenticationService.getCurrentAuthenticatedUser();
        log.info("Authenticated payment-service user resolved: userId={}", authUser.id());
        return authUser;
    }
}
