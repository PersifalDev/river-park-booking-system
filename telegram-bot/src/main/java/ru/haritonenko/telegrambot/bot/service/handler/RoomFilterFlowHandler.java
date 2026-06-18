package ru.haritonenko.telegrambot.bot.service.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.haritonenko.commonlibs.dto.category.type.RoomType;
import ru.haritonenko.telegrambot.bot.service.storage.BotConversationStore;
import ru.haritonenko.telegrambot.bot.state.AvailableRoomSearchDraft;
import ru.haritonenko.telegrambot.bot.state.ChatStateService;
import ru.haritonenko.telegrambot.bot.state.ChatStateType;
import ru.haritonenko.telegrambot.bot.util.BotKeyboardFactory;
import ru.haritonenko.telegrambot.bot.util.BotTextFactory;
import ru.haritonenko.telegrambot.config.BotFlowProperties;
import ru.haritonenko.telegrambot.service.BotMessageService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class RoomFilterFlowHandler {

    private static final ZoneId NOVOSIBIRSK_ZONE = ZoneId.of("Asia/Novosibirsk");
    private static final DateTimeFormatter FLEXIBLE_HUMAN_DATE_FORMATTER = DateTimeFormatter.ofPattern("d.M.uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    private final BotMessageService botMessageService;
    private final BotKeyboardFactory botKeyboardFactory;
    private final BotTextFactory botTextFactory;
    private final ChatStateService chatStateService;
    private final BotFlowProperties botFlowProperties;
    private final CatalogFlowHandler catalogFlowHandler;
    private final BotConversationStore conversationStore;

    public void startRoomFilter(Long chatId) {
        chatStateService.reset(chatId);
        conversationStore.removeAvailableRooms(chatId);
        chatStateService.setType(chatId, ChatStateType.WAITING_FILTER_GUESTS);
        botMessageService.sendText(chatId, botTextFactory.buildFilterStartMessage(
                botFlowProperties.filter().maxGuests(),
                botFlowProperties.booking().maxAdults(),
                botFlowProperties.booking().maxChildren()
        ), botKeyboardFactory.mainMenu());
    }

    public void handleRoomTypeSelection(Long chatId, Integer messageId, String data, boolean photoMessage) {
        AvailableRoomSearchDraft.AvailableRoomSearchDraftBuilder builder = chatStateService.get(chatId).availableRoomSearchDraft().toBuilder();
        String rawValue = data.substring("filter:room-type:".length());
        if ("skip".equalsIgnoreCase(rawValue)) {
            builder.roomType(null);
        } else {
            builder.roomType(RoomType.valueOf(rawValue));
        }

        chatStateService.updateAvailableRoomSearchDraft(chatId, builder.build());
        chatStateService.setType(chatId, ChatStateType.WAITING_FILTER_PRICE_FROM);

        if (photoMessage) {
            botMessageService.deleteMessage(chatId, messageId);
        } else if (messageId != null) {
            botMessageService.deleteMessage(chatId, messageId);
        }
        botMessageService.sendText(chatId, botTextFactory.buildAskPriceFromMessage(), botKeyboardFactory.mainMenu());
    }

    public void handleGuests(Long chatId, String text) {
        if (isSkip(text)) {
            AvailableRoomSearchDraft draft = chatStateService.get(chatId).availableRoomSearchDraft().toBuilder()
                    .guests(null)
                    .build();
            chatStateService.updateAvailableRoomSearchDraft(chatId, draft);
            chatStateService.setType(chatId, ChatStateType.WAITING_FILTER_CHECK_IN);
            botMessageService.sendText(chatId, botTextFactory.buildAskFilterCheckInMessage(), botKeyboardFactory.mainMenu());
            return;
        }

        Integer guests = parsePositiveInteger(text, botTextFactory.buildGuestsMustBeIntegerMessage(), chatId, botTextFactory.buildPositiveGuestsMessage());
        if (guests == null) {
            return;
        }
        if (guests > botFlowProperties.filter().maxGuests()) {
            botMessageService.sendText(chatId, maxGuestsMessage(), botKeyboardFactory.mainMenu());
            return;
        }

        AvailableRoomSearchDraft draft = chatStateService.get(chatId).availableRoomSearchDraft().toBuilder()
                .guests(guests)
                .build();
        chatStateService.updateAvailableRoomSearchDraft(chatId, draft);
        chatStateService.setType(chatId, ChatStateType.WAITING_FILTER_CHECK_IN);
        botMessageService.sendText(chatId, botTextFactory.buildAskFilterCheckInMessage(), botKeyboardFactory.mainMenu());
    }

    public void handleFilterCheckIn(Long chatId, String text) {
        if (isSkip(text)) {
            AvailableRoomSearchDraft draft = chatStateService.get(chatId).availableRoomSearchDraft().toBuilder()
                    .checkInDate(null)
                    .checkOutDate(null)
                    .build();
            chatStateService.updateAvailableRoomSearchDraft(chatId, draft);
            chatStateService.setType(chatId, ChatStateType.WAITING_FILTER_ROOM_TYPE);
            botMessageService.sendText(chatId, botTextFactory.buildAskRoomTypeMessage(), botKeyboardFactory.roomTypeSelection());
            return;
        }

        LocalDate checkInDate = parseDate(text, chatId);
        if (checkInDate == null) {
            return;
        }
        if (checkInDate.isBefore(today())) {
            botMessageService.sendText(chatId, botTextFactory.buildPastDateMessage(), botKeyboardFactory.mainMenu());
            return;
        }
        AvailableRoomSearchDraft draft = chatStateService.get(chatId).availableRoomSearchDraft().toBuilder()
                .checkInDate(checkInDate)
                .build();
        chatStateService.updateAvailableRoomSearchDraft(chatId, draft);
        chatStateService.setType(chatId, ChatStateType.WAITING_FILTER_CHECK_OUT);
        botMessageService.sendText(chatId, botTextFactory.buildAskFilterCheckOutMessage(checkInDate), botKeyboardFactory.mainMenu());
    }

    public void handleFilterCheckOut(Long chatId, String text) {
        if (isSkip(text)) {
            AvailableRoomSearchDraft currentDraft = chatStateService.get(chatId).availableRoomSearchDraft();
            AvailableRoomSearchDraft draft = currentDraft.toBuilder()
                    .checkInDate(null)
                    .checkOutDate(null)
                    .build();
            chatStateService.updateAvailableRoomSearchDraft(chatId, draft);
            chatStateService.setType(chatId, ChatStateType.WAITING_FILTER_ROOM_TYPE);
            botMessageService.sendText(chatId, botTextFactory.buildAskRoomTypeMessage(), botKeyboardFactory.roomTypeSelection());
            return;
        }

        LocalDate checkOutDate = parseDate(text, chatId);
        if (checkOutDate == null) {
            return;
        }
        AvailableRoomSearchDraft currentDraft = chatStateService.get(chatId).availableRoomSearchDraft();
        if (currentDraft.checkInDate() == null || !checkOutDate.isAfter(currentDraft.checkInDate())) {
            botMessageService.sendText(chatId, botTextFactory.buildCheckoutBeforeCheckinMessage(), botKeyboardFactory.mainMenu());
            return;
        }
        AvailableRoomSearchDraft draft = currentDraft.toBuilder()
                .checkOutDate(checkOutDate)
                .build();
        chatStateService.updateAvailableRoomSearchDraft(chatId, draft);
        rememberAvailabilityDraft(chatId, draft);
        chatStateService.setType(chatId, ChatStateType.WAITING_FILTER_ROOM_TYPE);
        botMessageService.sendText(chatId, botTextFactory.buildAskRoomTypeMessage(), botKeyboardFactory.roomTypeSelection());
    }

    public void handleRoomType(Long chatId, String text) {
        AvailableRoomSearchDraft.AvailableRoomSearchDraftBuilder builder = chatStateService.get(chatId).availableRoomSearchDraft().toBuilder();

        if (!isSkip(text)) {
            try {
                builder.roomType(parseRoomType(text));
            } catch (IllegalArgumentException exception) {
                botMessageService.sendText(chatId, botTextFactory.buildRoomTypeNotRecognizedMessage(), botKeyboardFactory.roomTypeSelection());
                return;
            }
        } else {
            builder.roomType(null);
        }

        chatStateService.updateAvailableRoomSearchDraft(chatId, builder.build());
        chatStateService.setType(chatId, ChatStateType.WAITING_FILTER_PRICE_FROM);
        botMessageService.sendText(chatId, botTextFactory.buildAskPriceFromMessage(), botKeyboardFactory.mainMenu());
    }

    public void handlePriceFrom(Long chatId, String text) {
        BigDecimal value = parseOptionalDecimalInRange(
                text,
                botTextFactory.buildMinPriceMustBeNumberMessage(),
                chatId,
                botFlowProperties.filter().minPrice(),
                botFlowProperties.filter().maxPrice(),
                priceRangeMessage()
        );
        if (value == null && !isSkip(text)) {
            return;
        }

        AvailableRoomSearchDraft draft = chatStateService.get(chatId).availableRoomSearchDraft().toBuilder()
                .priceFrom(value)
                .build();
        chatStateService.updateAvailableRoomSearchDraft(chatId, draft);
        chatStateService.setType(chatId, ChatStateType.WAITING_FILTER_PRICE_TO);
        botMessageService.sendText(chatId, botTextFactory.buildAskPriceToMessage(), botKeyboardFactory.mainMenu());
    }

    public void handlePriceTo(Long chatId, String text) {
        BigDecimal value = parseOptionalDecimalInRange(
                text,
                botTextFactory.buildMaxPriceMustBeNumberMessage(),
                chatId,
                botFlowProperties.filter().minPrice(),
                botFlowProperties.filter().maxPrice(),
                priceRangeMessage()
        );
        if (value == null && !isSkip(text)) {
            return;
        }
        BigDecimal priceFrom = chatStateService.get(chatId).availableRoomSearchDraft().priceFrom();
        if (value != null && priceFrom != null && value.compareTo(priceFrom) < 0) {
            botMessageService.sendText(chatId, botTextFactory.buildMaxPriceLowerThanMinMessage(), botKeyboardFactory.mainMenu());
            return;
        }

        AvailableRoomSearchDraft draft = chatStateService.get(chatId).availableRoomSearchDraft().toBuilder()
                .priceTo(value)
                .build();
        chatStateService.updateAvailableRoomSearchDraft(chatId, draft);
        chatStateService.setType(chatId, ChatStateType.WAITING_FILTER_MIN_AREA);
        botMessageService.sendText(chatId, botTextFactory.buildAskMinAreaMessage(), botKeyboardFactory.mainMenu());
    }

    public void handleMinArea(Long chatId, String text) {
        BigDecimal value = parseOptionalDecimalInRange(
                text,
                botTextFactory.buildMinAreaMustBeNumberMessage(),
                chatId,
                botFlowProperties.filter().minArea(),
                botFlowProperties.filter().maxArea(),
                areaRangeMessage()
        );
        if (value == null && !isSkip(text)) {
            return;
        }

        AvailableRoomSearchDraft draft = chatStateService.get(chatId).availableRoomSearchDraft().toBuilder()
                .minArea(value)
                .build();
        chatStateService.updateAvailableRoomSearchDraft(chatId, draft);
        chatStateService.setType(chatId, ChatStateType.IDLE);
        catalogFlowHandler.sendRoomsPage(chatId, 0, null, null, true);
    }

    private void rememberAvailabilityDraft(Long chatId, AvailableRoomSearchDraft draft) {
        if (draft != null && isValidAvailabilityPeriod(draft.checkInDate(), draft.checkOutDate())) {
            conversationStore.putLastAvailabilityDraft(chatId, draft);
        }
    }

    private boolean isValidAvailabilityPeriod(LocalDate checkInDate, LocalDate checkOutDate) {
        return checkInDate != null
                && checkOutDate != null
                && !checkInDate.isBefore(today())
                && checkOutDate.isAfter(checkInDate);
    }

    private LocalDate today() {
        return LocalDate.now(NOVOSIBIRSK_ZONE);
    }

    private boolean isSkip(String text) {
        return "-".equals(text.trim());
    }

    private Integer parsePositiveInteger(String text, String errorMessage, Long chatId, String notPositiveMessage) {
        try {
            int value = Integer.parseInt(text.trim());
            if (value < 0) {
                botMessageService.sendText(chatId, botTextFactory.buildNegativeValueMessage(), botKeyboardFactory.mainMenu());
                return null;
            }
            if (value == 0) {
                botMessageService.sendText(chatId, notPositiveMessage, botKeyboardFactory.mainMenu());
                return null;
            }
            return value;
        } catch (NumberFormatException exception) {
            botMessageService.sendText(chatId, errorMessage, botKeyboardFactory.mainMenu());
            return null;
        }
    }

    private BigDecimal parseOptionalDecimalInRange(
            String text,
            String errorMessage,
            Long chatId,
            BigDecimal minValue,
            BigDecimal maxValue,
            String rangeMessage
    ) {
        if (isSkip(text)) {
            return null;
        }

        try {
            BigDecimal value = new BigDecimal(text.trim());
            if (value.signum() < 0) {
                botMessageService.sendText(chatId, botTextFactory.buildNegativeValueMessage(), botKeyboardFactory.mainMenu());
                return null;
            }
            if (minValue != null && value.compareTo(minValue) < 0) {
                botMessageService.sendText(chatId, rangeMessage, botKeyboardFactory.mainMenu());
                return null;
            }
            if (maxValue != null && value.compareTo(maxValue) > 0) {
                botMessageService.sendText(chatId, rangeMessage, botKeyboardFactory.mainMenu());
                return null;
            }
            return value;
        } catch (NumberFormatException exception) {
            botMessageService.sendText(chatId, errorMessage, botKeyboardFactory.mainMenu());
            return null;
        }
    }

    private LocalDate parseDate(String text, Long chatId) {
        String value = text.trim();
        try {
            if (value.matches("\\d{1,2}\\.\\d{1,2}\\.\\d{4}")) {
                return LocalDate.parse(value, FLEXIBLE_HUMAN_DATE_FORMATTER);
            }
            return LocalDate.parse(value);
        } catch (DateTimeParseException exception) {
            botMessageService.sendText(chatId, botTextFactory.buildInvalidDateMessage(), botKeyboardFactory.mainMenu());
            return null;
        }
    }

    private RoomType parseRoomType(String text) {
        String normalized = text.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        normalized = switch (normalized) {
            case "STANDARD_PLUS", "STANDARD+" -> "STANDARD_PLUS";
            case "STANDARD_DOUBLE" -> "STANDARD_DOUBLE";
            case "BUSINESS_STUDIO" -> "BUSINESS_STUDIO";
            default -> normalized;
        };
        return RoomType.valueOf(normalized);
    }

    private String maxGuestsMessage() {
        return botTextFactory.buildMaxGuestsMessage(
                botFlowProperties.booking().maxGuests(),
                botFlowProperties.booking().maxAdults(),
                botFlowProperties.booking().maxChildren()
        );
    }

    private String priceRangeMessage() {
        return botTextFactory.buildPriceRangeMessage(
                botFlowProperties.filter().minPrice(),
                botFlowProperties.filter().maxPrice()
        );
    }

    private String areaRangeMessage() {
        return botTextFactory.buildAreaRangeMessage(
                botFlowProperties.filter().minArea(),
                botFlowProperties.filter().maxArea()
        );
    }
}
