package ru.haritonenko.telegrambot.client;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.haritonenko.telegrambot.dto.payment.BotPaymentResponseDto;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PaymentClient {

    private final RestClient paymentRestClient;

    public BotPaymentResponseDto getPaymentByBookingId(String jwt, UUID bookingId) {
        return paymentRestClient.get()
                .uri("/payments/booking/{bookingId}", bookingId)
                .header(HttpHeaders.AUTHORIZATION, bearer(jwt))
                .retrieve()
                .body(BotPaymentResponseDto.class);
    }

    public BotPaymentResponseDto confirmPayment(String jwt, UUID bookingId) {
        return paymentRestClient.patch()
                .uri("/payments/booking/{bookingId}/confirm", bookingId)
                .header(HttpHeaders.AUTHORIZATION, bearer(jwt))
                .retrieve()
                .body(BotPaymentResponseDto.class);
    }

    public BotPaymentResponseDto cancelPayment(String jwt, UUID bookingId) {
        return paymentRestClient.patch()
                .uri("/payments/booking/{bookingId}/cancel", bookingId)
                .header(HttpHeaders.AUTHORIZATION, bearer(jwt))
                .retrieve()
                .body(BotPaymentResponseDto.class);
    }

    private String bearer(String jwt) {
        return "Bearer " + jwt;
    }
}
