package ru.haritonenko.telegrambot.bot.service.strategy;

@FunctionalInterface
public interface MenuActionStrategy {
    void handle(Long chatId, String text);
}
