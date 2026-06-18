package ru.haritonenko.telegrambot.bot.service.strategy;

@FunctionalInterface
public interface CallbackActionStrategy {
    void handle(CallbackContext context);
}
