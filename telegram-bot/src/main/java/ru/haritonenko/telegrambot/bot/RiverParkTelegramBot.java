package ru.haritonenko.telegrambot.bot;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.longpolling.interfaces.LongPollingUpdateConsumer;
import org.telegram.telegrambots.longpolling.starter.SpringLongPollingBot;
import org.telegram.telegrambots.longpolling.util.LongPollingSingleThreadUpdateConsumer;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.haritonenko.telegrambot.bot.service.BotUpdateService;
import ru.haritonenko.telegrambot.config.BotProperties;

@Component
@RequiredArgsConstructor
public class RiverParkTelegramBot implements SpringLongPollingBot, LongPollingSingleThreadUpdateConsumer {

    private final BotProperties botProperties;
    private final BotUpdateService botUpdateService;

    @Override
    public String getBotToken() {
        return botProperties.token();
    }

    @Override
    public LongPollingUpdateConsumer getUpdatesConsumer() {
        return this;
    }

    @Override
    public void consume(Update update) {
        botUpdateService.handle(update);
    }
}
