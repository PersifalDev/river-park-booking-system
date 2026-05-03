package ru.haritonenko.telegrambot.client;

import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.haritonenko.telegrambot.dto.common.BotPageResponse;
import ru.haritonenko.telegrambot.dto.notification.BotNotificationResponseDto;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationClient {

    private static final ParameterizedTypeReference<BotPageResponse<BotNotificationResponseDto>> NOTIFICATION_PAGE_TYPE =
            new ParameterizedTypeReference<>() {};

    private final RestClient notificationRestClient;

    public List<BotNotificationResponseDto> getUnreadNotifications(String jwt, int pageNumber, int pageSize) {
        BotPageResponse<BotNotificationResponseDto> response = notificationRestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/notifications/unread")
                        .queryParam("pageNumber", pageNumber)
                        .queryParam("pageSize", pageSize)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, bearer(jwt))
                .retrieve()
                .body(NOTIFICATION_PAGE_TYPE);
        return response == null || response.content() == null ? List.of() : response.content();
    }

    public void markAsRead(String jwt, UUID notificationId) {
        notificationRestClient.patch()
                .uri("/notifications/{notificationId}/read", notificationId)
                .header(HttpHeaders.AUTHORIZATION, bearer(jwt))
                .retrieve()
                .toBodilessEntity();
    }

    public void markAllAsRead(String jwt) {
        notificationRestClient.patch()
                .uri("/notifications/read-all")
                .header(HttpHeaders.AUTHORIZATION, bearer(jwt))
                .retrieve()
                .toBodilessEntity();
    }

    private String bearer(String jwt) {
        return "Bearer " + jwt;
    }
}
