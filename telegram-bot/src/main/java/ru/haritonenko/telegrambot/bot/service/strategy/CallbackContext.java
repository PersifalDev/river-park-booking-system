package ru.haritonenko.telegrambot.bot.service.strategy;

public record CallbackContext(
        Long chatId,
        Integer messageId,
        String data,
        boolean photoMessage
) {
}
