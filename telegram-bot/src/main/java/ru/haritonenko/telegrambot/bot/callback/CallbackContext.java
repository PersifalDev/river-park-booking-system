package ru.haritonenko.telegrambot.bot.callback;

public record CallbackContext(
        Long chatId,
        Integer messageId,
        String data,
        boolean photoMessage
) {
}
