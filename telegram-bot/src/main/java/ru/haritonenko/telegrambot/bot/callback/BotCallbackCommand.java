package ru.haritonenko.telegrambot.bot.callback;

public enum BotCallbackCommand {
    NOOP,
    MAIN_MENU,
    PICK_ROOM,
    ALL_ROOMS,
    MY_BOOKINGS,
    INACTIVE_BOOKINGS,
    EARLY_BOOKINGS,
    BOOKING_HISTORY,
    CLEAR_INACTIVE_BOOKINGS,
    CLEAR_COMPLETED_BOOKINGS,
    BOOKING_PAGE,
    INACTIVE_BOOKING_PAGE,
    EARLY_BOOKING_PAGE,
    HISTORY_BOOKING_PAGE,
    NOTIFICATIONS,
    SERVICES,
    RULES,
    CONTACTS,
    FILTER_ROOM_TYPE,
    ROOMS_PAGE,
    FILTERED_ROOMS_PAGE,
    ROOM_VIEW,
    ROOM_PHOTOS,
    ROOM_PHOTO_INDEX,
    SERVICE_VIEW,
    SERVICES_PAGE,
    START_BOOKING,
    BOOKING_TARIFF,
    BOOKING_VIEW,
    BOOKING_CANCEL,
    PAYMENT_CONFIRM,
    PAYMENT_CANCEL,
    NOTIFICATION_READ,
    NOTIFICATION_READ_ALL,
    RULES_FILE,
    UNKNOWN;

    public static BotCallbackCommand fromData(String data) {
        if (data == null || data.isBlank()) {
            return UNKNOWN;
        }

        return switch (data) {
            case "noop" -> NOOP;
            case "menu:main" -> MAIN_MENU;
            case "menu:pick-room" -> PICK_ROOM;
            case "menu:all-rooms" -> ALL_ROOMS;
            case "menu:my-bookings" -> MY_BOOKINGS;
            case "booking:inactive" -> INACTIVE_BOOKINGS;
            case "booking:early" -> EARLY_BOOKINGS;
            case "booking:history" -> BOOKING_HISTORY;
            case "booking:clear:inactive" -> CLEAR_INACTIVE_BOOKINGS;
            case "booking:clear:completed" -> CLEAR_COMPLETED_BOOKINGS;
            case "menu:notifications" -> NOTIFICATIONS;
            case "menu:services" -> SERVICES;
            case "menu:rules" -> RULES;
            case "menu:contacts" -> CONTACTS;
            case "notification:read-all" -> NOTIFICATION_READ_ALL;
            case "rules:file" -> RULES_FILE;
            default -> fromPrefix(data);
        };
    }

    private static BotCallbackCommand fromPrefix(String data) {
        if (data.startsWith("booking:inactive:page:")) {
            return INACTIVE_BOOKING_PAGE;
        }
        if (data.startsWith("booking:early:page:")) {
            return EARLY_BOOKING_PAGE;
        }
        if (data.startsWith("booking:history:page:")) {
            return HISTORY_BOOKING_PAGE;
        }
        if (data.startsWith("booking:page:")) {
            return BOOKING_PAGE;
        }
        if (data.startsWith("filter:room-type:")) {
            return FILTER_ROOM_TYPE;
        }
        if (data.startsWith("rooms:filter:page:")) {
            return FILTERED_ROOMS_PAGE;
        }
        if (data.startsWith("rooms:page:")) {
            return ROOMS_PAGE;
        }
        if (data.startsWith("room:view:")) {
            return ROOM_VIEW;
        }
        if (data.startsWith("room:photos:")) {
            return ROOM_PHOTOS;
        }
        if (data.startsWith("room:photo:index:")) {
            return ROOM_PHOTO_INDEX;
        }
        if (data.startsWith("service:view:")) {
            return SERVICE_VIEW;
        }
        if (data.startsWith("services:page:")) {
            return SERVICES_PAGE;
        }
        if (data.startsWith("booking:start:")) {
            return START_BOOKING;
        }
        if (data.startsWith("booking:tariff:")) {
            return BOOKING_TARIFF;
        }
        if (data.startsWith("booking:view:")) {
            return BOOKING_VIEW;
        }
        if (data.startsWith("booking:cancel:")) {
            return BOOKING_CANCEL;
        }
        if (data.startsWith("payment:confirm:")) {
            return PAYMENT_CONFIRM;
        }
        if (data.startsWith("payment:cancel:")) {
            return PAYMENT_CANCEL;
        }
        if (data.startsWith("notification:read:")) {
            return NOTIFICATION_READ;
        }
        return UNKNOWN;
    }
}
