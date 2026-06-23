package ru.haritonenko.telegrambot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "bot.welcome")
public record BotWelcomeProperties(
        boolean enabled,
        String photoPath,
        String photoFileName,
        String stickerFileId
) {
}
