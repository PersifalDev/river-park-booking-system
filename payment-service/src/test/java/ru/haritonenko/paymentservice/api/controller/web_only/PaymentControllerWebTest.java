package ru.haritonenko.paymentservice.api.controller.web_only;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.haritonenko.commonlibs.security.authorization.user.AuthUser;
import ru.haritonenko.paymentservice.api.controller.PaymentController;
import ru.haritonenko.paymentservice.api.dto.filter.PaymentPageFilter;
import ru.haritonenko.paymentservice.domain.Payment;
import ru.haritonenko.paymentservice.domain.mapper.PaymentMapper;
import ru.haritonenko.paymentservice.domain.service.PaymentService;
import ru.haritonenko.paymentservice.domain.status.PaymentStatus;
import ru.haritonenko.paymentservice.security.jwt.manager.JwtTokenManager;
import ru.haritonenko.paymentservice.security.service.AuthenticationService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = PaymentController.class)
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerWebTest {

    private static final Long USER_ID = 10L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentService paymentService;

    @MockitoBean
    private PaymentMapper paymentMapper;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private JwtTokenManager jwtTokenManager;

    private Payment payment;

    @BeforeEach
    void setUp() {
        payment = payment(PaymentStatus.PENDING);
        when(authenticationService.getCurrentAuthenticatedUser())
                .thenReturn(AuthUser.builder().id(USER_ID).login("watson").role("USER").build());
        when(paymentMapper.toDto(any(Payment.class))).thenCallRealMethod();
    }

    @Test
    void shouldGetPaymentByBookingIdForAuthenticatedUser() throws Exception {
        when(paymentService.getPaymentByBookingIdAndUserId(payment.bookingId(), USER_ID)).thenReturn(payment);

        mockMvc.perform(get("/payments/booking/{bookingId}", payment.bookingId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bookingId").value(payment.bookingId().toString()))
                .andExpect(jsonPath("$.status").value("PENDING"));

        verify(paymentService).getPaymentByBookingIdAndUserId(payment.bookingId(), USER_ID);
    }

    @Test
    void shouldGetAllPaymentsForAuthenticatedUser() throws Exception {
        when(paymentService.getAllPaymentsByUserId(eq(USER_ID), any(PaymentPageFilter.class)))
                .thenReturn(new PageImpl<>(List.of(payment)));

        mockMvc.perform(get("/payments")
                        .param("pageNumber", "0")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("PENDING"));

        verify(paymentService).getAllPaymentsByUserId(eq(USER_ID), any(PaymentPageFilter.class));
    }

    @Test
    void shouldConfirmPayment() throws Exception {
        Payment confirmed = payment(PaymentStatus.CONFIRMED);
        when(paymentService.confirmPaymentByBookingIdAndUserId(payment.bookingId(), USER_ID)).thenReturn(confirmed);

        mockMvc.perform(patch("/payments/booking/{bookingId}/confirm", payment.bookingId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        verify(paymentService).confirmPaymentByBookingIdAndUserId(payment.bookingId(), USER_ID);
    }

    private Payment payment(PaymentStatus status) {
        return new Payment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BK-1",
                USER_ID,
                BigDecimal.valueOf(5000),
                status,
                "PAY_ON_ARRIVAL",
                "Оплата при заселении",
                "+7 383 000-00-00",
                "Оплатите на стойке регистрации",
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        );
    }
}
