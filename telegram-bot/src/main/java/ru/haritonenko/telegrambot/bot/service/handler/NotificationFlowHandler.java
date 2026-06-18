package ru.haritonenko.telegrambot.bot.service.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.haritonenko.telegrambot.bot.util.BotKeyboardFactory;
import ru.haritonenko.telegrambot.bot.util.BotTextFactory;
import ru.haritonenko.telegrambot.client.NotificationClient;
import ru.haritonenko.telegrambot.config.BotFlowProperties;
import ru.haritonenko.telegrambot.dto.notification.BotNotificationResponseDto;
import ru.haritonenko.telegrambot.service.BotMessageService;
import ru.haritonenko.telegrambot.service.auth.BotAuthService;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class NotificationFlowHandler {

    private final NotificationClient notificationClient;
    private final BotAuthService botAuthService;
    private final BotMessageService botMessageService;
    private final BotKeyboardFactory botKeyboardFactory;
    private final BotTextFactory botTextFactory;
    private final BotFlowProperties botFlowProperties;

    public void pushUnreadNotifications(Long chatId, boolean notifyIfEmpty) {
        String jwt = botAuthService.getJwt(chatId);
        List<BotNotificationResponseDto> notifications = notificationClient.getUnreadNotifications(
                jwt,
                0,
                botFlowProperties.pagination().notificationsPageSize()
        );
        if (notifications.isEmpty()) {
            if (notifyIfEmpty) {
                botMessageService.sendText(chatId, botTextFactory.buildNoUnreadNotificationsMessage(), botKeyboardFactory.mainMenu());
            }
            return;
        }
        for (BotNotificationResponseDto notification : notifications) {
            botMessageService.sendText(chatId, botTextFactory.buildNotificationMessage(notification), botKeyboardFactory.mainMenu());
            notificationClient.markAsRead(jwt, notification.id());
        }
    }

    public void markNotificationRead(Long chatId, UUID notificationId, Integer messageId, boolean photoMessage) {
        String jwt = botAuthService.getJwt(chatId);
        notificationClient.markAsRead(jwt, notificationId);
        if (photoMessage) {
            botMessageService.deleteMessage(chatId, messageId);
            botMessageService.sendText(chatId, botTextFactory.buildNotificationMarkedReadMessage(), botKeyboardFactory.mainMenu());
            return;
        }
        botMessageService.editText(chatId, messageId, botTextFactory.buildNotificationMarkedReadMessage(), botKeyboardFactory.inlineMainMenu());
    }

    public void markAllNotificationsRead(Long chatId, Integer messageId, boolean photoMessage) {
        String jwt = botAuthService.getJwt(chatId);
        notificationClient.markAllAsRead(jwt);
        if (photoMessage) {
            botMessageService.deleteMessage(chatId, messageId);
            botMessageService.sendText(chatId, botTextFactory.buildAllNotificationsMarkedReadMessage(), botKeyboardFactory.mainMenu());
            return;
        }
        botMessageService.editText(chatId, messageId, botTextFactory.buildAllNotificationsMarkedReadMessage(), botKeyboardFactory.inlineMainMenu());
    }
}
