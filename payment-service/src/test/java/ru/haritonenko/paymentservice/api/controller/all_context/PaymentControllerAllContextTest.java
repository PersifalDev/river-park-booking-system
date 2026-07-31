package ru.haritonenko.paymentservice.api.controller.all_context;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.haritonenko.commonlibs.security.authorization.user.AuthUser;
import ru.haritonenko.paymentservice.domain.Payment;
import ru.haritonenko.paymentservice.domain.mapper.PaymentMapper;
import ru.haritonenko.paymentservice.domain.service.PaymentService;
import ru.haritonenko.paymentservice.domain.status.PaymentStatus;
import ru.haritonenko.paymentservice.security.jwt.manager.JwtTokenManager;
import ru.haritonenko.paymentservice.security.service.AuthenticationService;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:payment-controller-context;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.kafka.listener.auto-startup=false",
        "spring.liquibase.enabled=false",
        "app.payment.outbox.enabled=false"
})
@AutoConfigureMockMvc(addFilters = false)
class PaymentControllerAllContextTest {

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

    @Test
    void shouldLoadApplicationContextAndGetPaymentByBookingId() throws Exception {
        Payment payment = payment();
        when(authenticationService.getCurrentAuthenticatedUser())
                .thenReturn(AuthUser.builder().id(USER_ID).login("watson").role("USER").build());
        when(paymentService.getPaymentByBookingIdAndUserId(payment.bookingId(), USER_ID)).thenReturn(payment);
        when(paymentMapper.toDto(any(Payment.class))).thenCallRealMethod();

        mockMvc.perform(get("/payments/booking/{bookingId}", payment.bookingId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"));
    }

    private Payment payment() {
        return new Payment(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "BK-1",
                USER_ID,
                BigDecimal.valueOf(5000),
                PaymentStatus.PENDING,
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
