package ru.haritonenko.telegrambot.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import ru.haritonenko.telegrambot.bot.service.BotUpdateService;
import ru.haritonenko.telegrambot.service.auth.BotAuthService;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationPollingService {

    private final BotAuthService botAuthService;
    private final BotUpdateService botUpdateService;

    @Scheduled(fixedDelayString = "${bot.notification.poll-interval-ms:15000}")
    public void pollUnreadNotifications() {
        for (Long chatId : botAuthService.getActiveChatIds()) {
            try {
                botUpdateService.pushUnreadNotifications(chatId, false);
            } catch (Exception exception) {
                log.warn("Failed to push unread notifications for chatId={}", chatId, exception);
            }
        }
    }
}
