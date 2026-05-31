package ru.haritonenko.paymentservice.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Payments", description = "Учебные платежи и подтверждение оплаты на месте")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final PaymentService paymentService;
    private final PaymentMapper paymentMapper;
    private final AuthenticationService authenticationService;

    @GetMapping("/booking/{bookingId}")
    @Operation(summary = "Получить платеж по брони")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Платеж найден"),
            @ApiResponse(responseCode = "404", description = "Платеж не найден")
    })
    public ResponseEntity<PaymentResponseDto> getPaymentByBookingId(
            @Parameter(description = "UUID брони") @PathVariable UUID bookingId
    ) {
        Long authUserId = getAuthenticatedUser().id();
        log.info("Request for getting payment by bookingId={} for userId={}", bookingId, authUserId);
        return ResponseEntity.ok(paymentMapper.toDto(paymentService.getPaymentByBookingIdAndUserId(bookingId, authUserId)));
    }

    @GetMapping
    @Operation(summary = "Получить платежи пользователя")
    @ApiResponse(responseCode = "200", description = "Страница платежей")
    public ResponseEntity<Page<PaymentResponseDto>> getAllPayments(@ModelAttribute PaymentPageFilter pageFilter) {
        Long authUserId = getAuthenticatedUser().id();
        log.info("Request for getting all payments by userId={}", authUserId);
        return ResponseEntity.ok(paymentService.getAllPaymentsByUserId(authUserId, pageFilter).map(paymentMapper::toDto));
    }

    @PatchMapping("/booking/{bookingId}/confirm")
    @Operation(summary = "Подтвердить оплату на месте")
    @ApiResponse(responseCode = "200", description = "Платеж подтвержден")
    public ResponseEntity<PaymentResponseDto> confirmPayment(
            @Parameter(description = "UUID брони") @PathVariable UUID bookingId
    ) {
        Long authUserId = getAuthenticatedUser().id();
        log.info("Request for confirming payment by bookingId={} for userId={}", bookingId, authUserId);
        return ResponseEntity.ok(paymentMapper.toDto(paymentService.confirmPaymentByBookingIdAndUserId(bookingId, authUserId)));
    }

    @PatchMapping("/booking/{bookingId}/cancel")
    @Operation(summary = "Отменить подтверждение оплаты")
    @ApiResponse(responseCode = "200", description = "Платеж отменен")
    public ResponseEntity<PaymentResponseDto> cancelPayment(
            @Parameter(description = "UUID брони") @PathVariable UUID bookingId
    ) {
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
