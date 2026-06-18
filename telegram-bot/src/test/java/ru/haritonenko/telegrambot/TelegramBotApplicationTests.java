package ru.haritonenko.telegrambot;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.autoconfigure.exclude=org.telegram.telegrambots.longpolling.starter.TelegramBotStarterConfiguration",
        "telegram.bot.token=test-token",
        "telegram.bot.username=test-bot",
        "telegram.bot.admin-contact=test-admin"
})
class TelegramBotApplicationTests {

    @Test
    void contextLoads() {
    }

}
