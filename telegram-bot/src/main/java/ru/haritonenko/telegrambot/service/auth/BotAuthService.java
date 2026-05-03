package ru.haritonenko.telegrambot.service.auth;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpStatusCodeException;
import ru.haritonenko.commonlibs.security.authorization.user.UserCredentials;
import ru.haritonenko.telegrambot.client.UserClient;
import ru.haritonenko.telegrambot.dto.auth.BotJwtResponse;
import ru.haritonenko.telegrambot.dto.auth.BotUserRegistrationRequest;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class BotAuthService {

    private final UserClient userClient;

    private final ConcurrentHashMap<Long, String> jwtByChatId = new ConcurrentHashMap<>();
    private final Set<Long> activeChatIds = ConcurrentHashMap.newKeySet();
    private final Set<Long> knownRegisteredChatIds = ConcurrentHashMap.newKeySet();

    public String getJwt(Long chatId) {
        touchChat(chatId);
        ensureRegistered(chatId);
        String jwt = jwtByChatId.get(chatId);
        if (jwt != null && !jwt.isBlank()) {
            return jwt;
        }
        String refreshedJwt = authenticate(chatId);
        jwtByChatId.put(chatId, refreshedJwt);
        return refreshedJwt;
    }

    public void invalidate(Long chatId) {
        if (chatId != null) {
            jwtByChatId.remove(chatId);
        }
    }

    public void touchChat(Long chatId) {
        if (chatId != null) {
            activeChatIds.add(chatId);
        }
    }

    public void ensureRegistered(Long chatId) {
        if (chatId == null || knownRegisteredChatIds.contains(chatId)) {
            return;
        }

        try {
            userClient.register(buildRegistration(chatId));
            knownRegisteredChatIds.add(chatId);
            log.info("Telegram user registered in user-service for chatId={}", chatId);
        } catch (HttpStatusCodeException exception) {
            if (HttpStatus.CONFLICT.equals(exception.getStatusCode())) {
                knownRegisteredChatIds.add(chatId);
                log.info("Telegram user already registered for chatId={}", chatId);
                return;
            }
            if (HttpStatus.BAD_REQUEST.equals(exception.getStatusCode())) {
                log.warn("Telegram user registration rejected for chatId={}", chatId, exception);
                return;
            }
            throw exception;
        }
    }

    public Set<Long> getActiveChatIds() {
        return Set.copyOf(activeChatIds);
    }

    private String authenticate(Long chatId) {
        UserCredentials credentials = buildCredentials(chatId);

        try {
            BotJwtResponse jwtResponse = userClient.authenticate(credentials);
            if (jwtResponse != null && jwtResponse.jwt() != null && !jwtResponse.jwt().isBlank()) {
                knownRegisteredChatIds.add(chatId);
                return jwtResponse.jwt();
            }
        } catch (HttpStatusCodeException exception) {
            if (!shouldRegisterAndRetry(exception)) {
                throw exception;
            }
        }

        ensureRegistered(chatId);

        BotJwtResponse jwtResponse = userClient.authenticate(credentials);
        if (jwtResponse == null || jwtResponse.jwt() == null || jwtResponse.jwt().isBlank()) {
            throw new IllegalStateException("JWT not received from user-service");
        }
        knownRegisteredChatIds.add(chatId);
        return jwtResponse.jwt();
    }

    private boolean shouldRegisterAndRetry(HttpStatusCodeException exception) {
        return HttpStatus.UNAUTHORIZED.equals(exception.getStatusCode())
                || HttpStatus.NOT_FOUND.equals(exception.getStatusCode())
                || HttpStatus.INTERNAL_SERVER_ERROR.equals(exception.getStatusCode());
    }

    private BotUserRegistrationRequest buildRegistration(Long chatId) {
        return new BotUserRegistrationRequest(login(chatId), secret(chatId));
    }

    private UserCredentials buildCredentials(Long chatId) {
        return new UserCredentials(login(chatId), secret(chatId));
    }

    private String login(Long chatId) {
        return "tg_" + Math.abs(chatId);
    }

    private String secret(Long chatId) {
        return "river_bot_key_" + Math.abs(chatId);
    }
}
