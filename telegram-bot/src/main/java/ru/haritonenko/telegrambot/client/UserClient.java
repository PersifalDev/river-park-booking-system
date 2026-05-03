package ru.haritonenko.telegrambot.client;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import ru.haritonenko.commonlibs.security.authorization.user.UserCredentials;
import ru.haritonenko.telegrambot.dto.auth.BotJwtResponse;
import ru.haritonenko.telegrambot.dto.auth.BotUserRegistrationRequest;

@Component
@RequiredArgsConstructor
public class UserClient {

    private final RestClient userRestClient;

    public void register(BotUserRegistrationRequest request) {
        userRestClient.post()
                .uri("/users")
                .body(request)
                .retrieve()
                .toBodilessEntity();
    }

    public BotJwtResponse authenticate(UserCredentials credentials) {
        return userRestClient.post()
                .uri("/users/auth")
                .body(credentials)
                .retrieve()
                .body(BotJwtResponse.class);
    }
}
