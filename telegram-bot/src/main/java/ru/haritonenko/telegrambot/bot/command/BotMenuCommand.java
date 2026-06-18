package ru.haritonenko.telegrambot.bot.command;

import ru.haritonenko.telegrambot.config.BotKeyboardMessagesProperties;

import java.util.Arrays;
import java.util.Optional;

public enum BotMenuCommand {
    PICK_ROOM,
    ALL_ROOMS,
    MY_BOOKINGS,
    NOTIFICATIONS,
    FIND_ROOM,
    SERVICES,
    FIND_SERVICE,
    RULES,
    CONTACTS,
    SITE;

    public static Optional<BotMenuCommand> fromText(String text, BotKeyboardMessagesProperties labels) {
        if (text == null || text.isBlank()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(command -> command.label(labels).equalsIgnoreCase(text.trim()))
                .findFirst();
    }

    public String label(BotKeyboardMessagesProperties labels) {
        return switch (this) {
            case PICK_ROOM -> labels.pickRoom();
            case ALL_ROOMS -> labels.allRooms();
            case MY_BOOKINGS -> labels.myBookings();
            case NOTIFICATIONS -> labels.notifications();
            case FIND_ROOM -> labels.findRoom();
            case SERVICES -> labels.services();
            case FIND_SERVICE -> labels.findService();
            case RULES -> labels.rules();
            case CONTACTS -> labels.contacts();
            case SITE -> labels.site();
        };
    }
}
