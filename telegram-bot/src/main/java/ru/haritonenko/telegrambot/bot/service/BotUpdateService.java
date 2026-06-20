package ru.haritonenko.telegrambot.bot.service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.haritonenko.commonlibs.dto.category.RoomCategoryResponseDto;
import ru.haritonenko.commonlibs.dto.error.ErrorMessageResponse;
import ru.haritonenko.commonlibs.dto.service.ServiceItemResponseDto;
import ru.haritonenko.telegrambot.bot.callback.BotCallbackCommand;
import ru.haritonenko.telegrambot.bot.command.BotMenuCommand;
import ru.haritonenko.telegrambot.bot.service.handler.*;
import ru.haritonenko.telegrambot.bot.service.storage.BotConversationStore;
import ru.haritonenko.telegrambot.bot.service.strategy.CallbackActionStrategy;
import ru.haritonenko.telegrambot.bot.service.strategy.CallbackContext;
import ru.haritonenko.telegrambot.bot.service.strategy.MenuActionStrategy;
import ru.haritonenko.telegrambot.bot.state.ChatStateService;
import ru.haritonenko.telegrambot.bot.state.ChatStateType;
import ru.haritonenko.telegrambot.bot.util.BotKeyboardFactory;
import ru.haritonenko.telegrambot.bot.util.BotTextFactory;
import ru.haritonenko.telegrambot.client.BookingClient;
import ru.haritonenko.telegrambot.client.CatalogClient;
import ru.haritonenko.telegrambot.config.*;
import ru.haritonenko.telegrambot.service.BotMessageService;
import ru.haritonenko.telegrambot.service.auth.BotAuthService;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class BotUpdateService {

    private final CatalogClient catalogClient;
    private final BookingClient bookingClient;
    private final BotAuthService botAuthService;
    private final BotMessageService botMessageService;
    private final BotKeyboardFactory botKeyboardFactory;
    private final BotTextFactory botTextFactory;
    private final ChatStateService chatStateService;
    private final BotProperties botProperties;
    private final BotFlowProperties botFlowProperties;
    private final BookingClientProperties bookingClientProperties;
    private final CatalogClientProperties catalogClientProperties;
    private final PaymentClientProperties paymentClientProperties;
    private final NotificationClientProperties notificationClientProperties;
    private final UserClientProperties userClientProperties;
    private final NotificationFlowHandler notificationFlowHandler;
    private final BookingHistoryFlowHandler bookingHistoryFlowHandler;
    private final BookingActionFlowHandler bookingActionFlowHandler;
    private final CatalogFlowHandler catalogFlowHandler;
    private final RoomFilterFlowHandler roomFilterFlowHandler;
    private final BookingCreationFlowHandler bookingCreationFlowHandler;

    private final Map<BotMenuCommand, MenuActionStrategy> menuActions = new EnumMap<>(BotMenuCommand.class);
    private final Map<BotCallbackCommand, CallbackActionStrategy> callbackActions = new EnumMap<>(BotCallbackCommand.class);
    private final BotConversationStore conversationStore;

    @PostConstruct
    void initActionStrategies() {
        registerMenuActions();
        registerCallbackActions();
    }

    private void registerMenuActions() {
        menuActions.put(BotMenuCommand.PICK_ROOM, (chatId, text) -> roomFilterFlowHandler.startRoomFilter(chatId));
        menuActions.put(BotMenuCommand.ALL_ROOMS, (chatId, text) -> {
            chatStateService.reset(chatId);
            catalogFlowHandler.sendRoomsPage(chatId, 0, null, null);
        });
        menuActions.put(BotMenuCommand.MY_BOOKINGS, (chatId, text) -> {
            chatStateService.reset(chatId);
            bookingHistoryFlowHandler.sendBookings(chatId, null, false);
        });
        menuActions.put(BotMenuCommand.NOTIFICATIONS, (chatId, text) -> {
            chatStateService.reset(chatId);
            botMessageService.sendText(chatId, botTextFactory.buildNotificationsAutomaticInfoMessage(), botKeyboardFactory.mainMenu());
            notificationFlowHandler.pushUnreadNotifications(chatId, false);
        });
        menuActions.put(BotMenuCommand.FIND_ROOM, (chatId, text) -> {
            chatStateService.reset(chatId);
            chatStateService.setType(chatId, ChatStateType.WAITING_ROOM_ID);
            List<RoomCategoryResponseDto> rooms = catalogClient.getRooms(0, botFlowProperties.pagination().roomPageSize()).content();
            botMessageService.sendText(chatId, botTextFactory.buildRoomSelectionMessage(rooms), botKeyboardFactory.mainMenu());
        });
        menuActions.put(BotMenuCommand.SERVICES, (chatId, text) -> {
            chatStateService.reset(chatId);
            catalogFlowHandler.sendServicesPage(chatId, 0, null);
        });
        menuActions.put(BotMenuCommand.FIND_SERVICE, (chatId, text) -> {
            chatStateService.reset(chatId);
            chatStateService.setType(chatId, ChatStateType.WAITING_SERVICE_ID);
            List<ServiceItemResponseDto> services = catalogClient.getServices(0, botFlowProperties.pagination().servicePageSize());
            botMessageService.sendText(chatId, botTextFactory.buildServicePrompt(services), botKeyboardFactory.mainMenu());
        });
        menuActions.put(BotMenuCommand.RULES, (chatId, text) -> catalogFlowHandler.sendRules(chatId));
        menuActions.put(BotMenuCommand.CONTACTS, (chatId, text) ->
                botMessageService.sendText(chatId, botTextFactory.buildContactsMessage(botProperties.adminContact()), botKeyboardFactory.mainMenu()));
        menuActions.put(BotMenuCommand.SITE, (chatId, text) ->
                botMessageService.sendText(chatId, botTextFactory.buildSiteMessage(), botKeyboardFactory.mainMenu()));
    }

    private void registerCallbackActions() {
        callbackActions.put(BotCallbackCommand.NOOP, context -> {
        });
        callbackActions.put(BotCallbackCommand.MAIN_MENU, context -> {
            chatStateService.reset(context.chatId());
            if (context.photoMessage()) {
                botMessageService.deleteMessage(context.chatId(), context.messageId());
                botMessageService.sendText(context.chatId(), botTextFactory.buildMenuMessage(), botKeyboardFactory.mainMenu());
                return;
            }
            botMessageService.editText(context.chatId(), context.messageId(), botTextFactory.buildMenuMessage(), botKeyboardFactory.inlineMainMenu());
        });
        callbackActions.put(BotCallbackCommand.PICK_ROOM, context -> {
            deletePhotoMessageIfNeeded(context);
            roomFilterFlowHandler.startRoomFilter(context.chatId());
        });
        callbackActions.put(BotCallbackCommand.ALL_ROOMS, context -> {
            chatStateService.reset(context.chatId());
            if (context.photoMessage()) {
                botMessageService.deleteMessage(context.chatId(), context.messageId());
                catalogFlowHandler.sendRoomsPage(context.chatId(), 0, null, null);
                return;
            }
            catalogFlowHandler.sendRoomsPage(context.chatId(), 0, context.messageId(), null);
        });
        callbackActions.put(BotCallbackCommand.MY_BOOKINGS, context -> {
            deletePhotoMessageIfNeeded(context);
            bookingHistoryFlowHandler.sendBookings(context.chatId(), context.photoMessage() ? null : context.messageId(), false);
        });
        callbackActions.put(BotCallbackCommand.INACTIVE_BOOKINGS, context -> {
            deletePhotoMessageIfNeeded(context);
            bookingHistoryFlowHandler.sendInactiveBookings(context.chatId(), context.photoMessage() ? null : context.messageId(), false, 0);
        });
        callbackActions.put(BotCallbackCommand.EARLY_BOOKINGS, context -> {
            deletePhotoMessageIfNeeded(context);
            bookingHistoryFlowHandler.sendEarlyBookings(context.chatId(), context.photoMessage() ? null : context.messageId(), false, 0);
        });
        callbackActions.put(BotCallbackCommand.BOOKING_HISTORY, context -> {
            deletePhotoMessageIfNeeded(context);
            bookingHistoryFlowHandler.sendHistoryBookings(context.chatId(), context.photoMessage() ? null : context.messageId(), false, 0);
        });
        callbackActions.put(BotCallbackCommand.CLEAR_INACTIVE_BOOKINGS, context -> {
            bookingClient.clearInactiveBookings(botAuthService.getJwt(context.chatId()));
            bookingHistoryFlowHandler.sendInactiveBookings(context.chatId(), context.photoMessage() ? null : context.messageId(), false, 0);
        });
        callbackActions.put(BotCallbackCommand.CLEAR_COMPLETED_BOOKINGS, context -> {
            bookingClient.clearCompletedBookings(botAuthService.getJwt(context.chatId()));
            bookingHistoryFlowHandler.sendBookings(context.chatId(), context.photoMessage() ? null : context.messageId(), false, 0);
        });
        callbackActions.put(BotCallbackCommand.BOOKING_PAGE, context -> bookingHistoryFlowHandler.sendBookings(context.chatId(), context.photoMessage() ? null : context.messageId(), context.photoMessage(), parseSuffixInt(context.data(), "booking:page:")));
        callbackActions.put(BotCallbackCommand.INACTIVE_BOOKING_PAGE, context -> bookingHistoryFlowHandler.sendInactiveBookings(context.chatId(), context.photoMessage() ? null : context.messageId(), context.photoMessage(), parseSuffixInt(context.data(), "booking:inactive:page:")));
        callbackActions.put(BotCallbackCommand.EARLY_BOOKING_PAGE, context -> bookingHistoryFlowHandler.sendEarlyBookings(context.chatId(), context.photoMessage() ? null : context.messageId(), context.photoMessage(), parseSuffixInt(context.data(), "booking:early:page:")));
        callbackActions.put(BotCallbackCommand.HISTORY_BOOKING_PAGE, context -> bookingHistoryFlowHandler.sendHistoryBookings(context.chatId(), context.photoMessage() ? null : context.messageId(), context.photoMessage(), parseSuffixInt(context.data(), "booking:history:page:")));
        callbackActions.put(BotCallbackCommand.NOTIFICATIONS, context -> {
            deletePhotoMessageIfNeeded(context);
            notificationFlowHandler.pushUnreadNotifications(context.chatId(), true);
        });
        callbackActions.put(BotCallbackCommand.SERVICES, context -> {
            chatStateService.reset(context.chatId());
            if (context.photoMessage()) {
                botMessageService.deleteMessage(context.chatId(), context.messageId());
                catalogFlowHandler.sendServicesPage(context.chatId(), 0, null);
                return;
            }
            catalogFlowHandler.sendServicesPage(context.chatId(), 0, context.messageId());
        });
        callbackActions.put(BotCallbackCommand.RULES, context -> {
            chatStateService.reset(context.chatId());
            String rulesMessage = botTextFactory.buildRulesMessage(catalogClient.getRuleDocument());
            if (context.photoMessage()) {
                botMessageService.deleteMessage(context.chatId(), context.messageId());
                botMessageService.sendText(context.chatId(), rulesMessage, botKeyboardFactory.rulesKeyboard());
                return;
            }
            botMessageService.editText(context.chatId(), context.messageId(), rulesMessage, botKeyboardFactory.rulesKeyboard());
        });
        callbackActions.put(BotCallbackCommand.CONTACTS, context -> {
            chatStateService.reset(context.chatId());
            String contacts = botTextFactory.buildContactsMessage(botProperties.adminContact());
            if (context.photoMessage()) {
                botMessageService.deleteMessage(context.chatId(), context.messageId());
                botMessageService.sendText(context.chatId(), contacts, botKeyboardFactory.mainMenu());
                return;
            }
            botMessageService.editText(context.chatId(), context.messageId(), contacts, botKeyboardFactory.inlineMainMenu());
        });
        callbackActions.put(BotCallbackCommand.FILTER_GUESTS, context -> roomFilterFlowHandler.requestGuests(context.chatId(), context.messageId(), context.photoMessage()));
        callbackActions.put(BotCallbackCommand.FILTER_DATES, context -> roomFilterFlowHandler.requestDates(context.chatId(), context.messageId(), context.photoMessage()));
        callbackActions.put(BotCallbackCommand.FILTER_PRICE, context -> roomFilterFlowHandler.requestPrice(context.chatId(), context.messageId(), context.photoMessage()));
        callbackActions.put(BotCallbackCommand.FILTER_AREA, context -> roomFilterFlowHandler.requestArea(context.chatId(), context.messageId(), context.photoMessage()));
        callbackActions.put(BotCallbackCommand.FILTER_SEARCH, context -> roomFilterFlowHandler.search(context.chatId(), context.messageId(), context.photoMessage()));
        callbackActions.put(BotCallbackCommand.FILTER_RESET, context -> roomFilterFlowHandler.resetFilter(context.chatId(), context.messageId(), context.photoMessage()));
        callbackActions.put(BotCallbackCommand.FILTER_ROOM_TYPE, context -> roomFilterFlowHandler.handleRoomTypeSelection(context.chatId(), context.messageId(), context.data(), context.photoMessage()));
        callbackActions.put(BotCallbackCommand.ROOMS_PAGE, context -> {
            int pageNumber = parseSuffixInt(context.data(), "rooms:page:");
            if (context.photoMessage()) {
                catalogFlowHandler.sendRoomsPage(context.chatId(), pageNumber, context.messageId(), null, false);
                return;
            }
            botMessageService.deleteMessage(context.chatId(), context.messageId());
            catalogFlowHandler.sendRoomsPage(context.chatId(), pageNumber, null, null, false);
        });
        callbackActions.put(BotCallbackCommand.FILTERED_ROOMS_PAGE, context -> {
            int pageNumber = parseSuffixInt(context.data(), "rooms:filter:page:");
            if (context.photoMessage()) {
                catalogFlowHandler.sendRoomsPage(context.chatId(), pageNumber, context.messageId(), null, true);
                return;
            }
            botMessageService.deleteMessage(context.chatId(), context.messageId());
            catalogFlowHandler.sendRoomsPage(context.chatId(), pageNumber, null, null, true);
        });
        callbackActions.put(BotCallbackCommand.ROOM_VIEW, context -> catalogFlowHandler.handleRoomViewCallback(context.chatId(), context.messageId(), context.data(), context.photoMessage()));
        callbackActions.put(BotCallbackCommand.ROOM_PHOTOS, context -> {
            String[] parts = context.data().split(":");
            Long roomId = Long.parseLong(parts[2]);
            int roomPageNumber = Integer.parseInt(parts[3]);
            boolean filtered = parts.length > 4 && "filter".equalsIgnoreCase(parts[4]);
            catalogFlowHandler.openPhotoGallery(context.chatId(), roomId, roomPageNumber, context.messageId(), context.photoMessage(), filtered);
        });
        callbackActions.put(BotCallbackCommand.ROOM_PHOTO_INDEX, context -> catalogFlowHandler.handlePhotoIndexCallback(context.chatId(), context.messageId(), context.data()));
        callbackActions.put(BotCallbackCommand.SERVICE_VIEW, context -> catalogFlowHandler.handleServiceViewCallback(context.chatId(), context.messageId(), context.data()));
        callbackActions.put(BotCallbackCommand.SERVICES_PAGE, context -> {
            int pageNumber = parseSuffixInt(context.data(), "services:page:");
            if (context.photoMessage()) {
                botMessageService.deleteMessage(context.chatId(), context.messageId());
                catalogFlowHandler.sendServicesPage(context.chatId(), pageNumber, null);
                return;
            }
            catalogFlowHandler.sendServicesPage(context.chatId(), pageNumber, context.messageId());
        });
        callbackActions.put(BotCallbackCommand.START_BOOKING, context -> bookingCreationFlowHandler.startBookingFlow(context.chatId(), Long.parseLong(context.data().substring("booking:start:".length()))));
        callbackActions.put(BotCallbackCommand.BOOKING_TARIFF, context -> bookingCreationFlowHandler.handleTariffSelection(context.chatId(), context.data()));
        callbackActions.put(BotCallbackCommand.BOOKING_VIEW, context -> bookingHistoryFlowHandler.showBookingDetails(context.chatId(), UUID.fromString(context.data().substring("booking:view:".length())), context.messageId(), context.photoMessage()));
        callbackActions.put(BotCallbackCommand.BOOKING_CANCEL, context -> bookingActionFlowHandler.cancelBooking(context.chatId(), UUID.fromString(context.data().substring("booking:cancel:".length())), context.messageId(), context.photoMessage()));
        callbackActions.put(BotCallbackCommand.PAYMENT_CONFIRM, context -> bookingActionFlowHandler.confirmPayment(context.chatId(), UUID.fromString(context.data().substring("payment:confirm:".length())), context.messageId(), context.photoMessage()));
        callbackActions.put(BotCallbackCommand.PAYMENT_CANCEL, context -> bookingActionFlowHandler.cancelBooking(context.chatId(), UUID.fromString(context.data().substring("payment:cancel:".length())), context.messageId(), context.photoMessage()));
        callbackActions.put(BotCallbackCommand.NOTIFICATION_READ, context -> notificationFlowHandler.markNotificationRead(context.chatId(), UUID.fromString(context.data().substring("notification:read:".length())), context.messageId(), context.photoMessage()));
        callbackActions.put(BotCallbackCommand.NOTIFICATION_READ_ALL, context -> notificationFlowHandler.markAllNotificationsRead(context.chatId(), context.messageId(), context.photoMessage()));
        callbackActions.put(BotCallbackCommand.RULES_FILE, context -> catalogFlowHandler.sendRuleFile(context.chatId()));
    }

    public void handle(Update update) {
        try {
            if (update.hasCallbackQuery()) {
                handleCallback(update);
                return;
            }

            if (!update.hasMessage() || !update.getMessage().hasText()) {
                return;
            }

            Long chatId = update.getMessage().getChatId();
            botAuthService.touchChat(chatId);
            botAuthService.ensureRegistered(chatId);
            String text = update.getMessage().getText().trim();
            log.info("Received message chatId={}, text={}", chatId, text);

        if ("/start".equalsIgnoreCase(text) || "/help".equalsIgnoreCase(text)) {
            chatStateService.reset(chatId);
            conversationStore.removeLastAvailabilityDraft(chatId);
            botMessageService.sendText(chatId, botTextFactory.buildStartMessage(), botKeyboardFactory.mainMenu());
            return;
        }

            if ("/site".equalsIgnoreCase(text)) {
                chatStateService.reset(chatId);
                botMessageService.sendText(chatId, botTextFactory.buildSiteMessage(), botKeyboardFactory.mainMenu());
                return;
            }

            if (handleMenuAction(chatId, text)) {
                return;
            }

            handleStateInput(chatId, text);
        } catch (RestClientResponseException exception) {
            log.error("Bot request failed", exception);
            if (exception.getStatusCode().value() == HttpStatus.UNAUTHORIZED.value() || exception.getStatusCode().value() == HttpStatus.FORBIDDEN.value()) {
                botAuthService.invalidate(resolveChatId(update));
            }
            botMessageService.sendText(
                    resolveChatId(update),
                    extractErrorMessage(exception, botTextFactory.buildUnexpectedErrorMessage()),
                    botKeyboardFactory.mainMenu()
            );
        } catch (ResourceAccessException exception) {
            log.error("Bot request failed because downstream service is unavailable", exception);
            botMessageService.sendText(
                    resolveChatId(update),
                    buildServiceUnavailableMessage(exception),
                    botKeyboardFactory.mainMenu()
            );
        } catch (Exception exception) {
            log.error("Unexpected bot error", exception);
            botMessageService.sendText(resolveChatId(update), botTextFactory.buildUnexpectedErrorMessage(), botKeyboardFactory.mainMenu());
        }
    }

    private boolean handleMenuAction(Long chatId, String text) {
        return botKeyboardFactory.resolveMenuCommand(text)
                .map(command -> {
                    menuActions.get(command).handle(chatId, text);
                    return true;
                })
                .orElse(false);
    }

    private void handleStateInput(Long chatId, String text) {
        ChatStateType stateType = chatStateService.get(chatId).type();

        switch (stateType) {
            case WAITING_ROOM_ID -> catalogFlowHandler.handleRoomId(chatId, text);
            case WAITING_SERVICE_ID -> catalogFlowHandler.handleServiceId(chatId, text);
            case WAITING_FILTER_GUESTS -> roomFilterFlowHandler.handleGuests(chatId, text);
            case WAITING_FILTER_ADULTS -> roomFilterFlowHandler.handleAdults(chatId, text);
            case WAITING_FILTER_CHILDREN -> roomFilterFlowHandler.handleChildren(chatId, text);
            case WAITING_FILTER_CHECK_IN -> roomFilterFlowHandler.handleFilterCheckIn(chatId, text);
            case WAITING_FILTER_CHECK_OUT -> roomFilterFlowHandler.handleFilterCheckOut(chatId, text);
            case WAITING_FILTER_ROOM_TYPE -> roomFilterFlowHandler.handleRoomType(chatId, text);
            case WAITING_FILTER_PRICE_FROM -> roomFilterFlowHandler.handlePriceFrom(chatId, text);
            case WAITING_FILTER_PRICE_TO -> roomFilterFlowHandler.handlePriceTo(chatId, text);
            case WAITING_FILTER_MIN_AREA -> roomFilterFlowHandler.handleMinArea(chatId, text);
            case WAITING_BOOKING_CHECK_IN -> bookingCreationFlowHandler.handleBookingCheckIn(chatId, text);
            case WAITING_BOOKING_CHECK_OUT -> bookingCreationFlowHandler.handleBookingCheckOut(chatId, text);
            case WAITING_BOOKING_ADULTS -> bookingCreationFlowHandler.handleBookingAdults(chatId, text);
            case WAITING_BOOKING_CHILDREN -> bookingCreationFlowHandler.handleBookingChildren(chatId, text);
            case WAITING_BOOKING_PROMO -> bookingCreationFlowHandler.handleBookingPromo(chatId, text);
            default -> botMessageService.sendText(chatId, botTextFactory.buildMenuMessage(), botKeyboardFactory.mainMenu());
        }
    }

    private void handleCallback(Update update) {
        String data = update.getCallbackQuery().getData();
        Long chatId = update.getCallbackQuery().getMessage().getChatId();
        botAuthService.touchChat(chatId);
        botAuthService.ensureRegistered(chatId);
        Integer messageId = update.getCallbackQuery().getMessage().getMessageId();
        boolean photoMessage = isPhotoMessage(update);

        botMessageService.answerCallback(update.getCallbackQuery().getId(), "");

        BotCallbackCommand command = BotCallbackCommand.fromData(data);
        CallbackActionStrategy action = callbackActions.get(command);
        if (action == null) {
            log.warn("Unknown callback data={}", data);
            return;
        }

        action.handle(new CallbackContext(chatId, messageId, data, photoMessage));
    }

    private Long resolveChatId(Update update) {
        if (update.hasMessage()) {
            return update.getMessage().getChatId();
        }
        if (update.hasCallbackQuery()) {
            return update.getCallbackQuery().getMessage().getChatId();
        }
        return null;
    }

    private String extractErrorMessage(RestClientResponseException exception, String defaultMessage) {
        try {
            ErrorMessageResponse error = exception.getResponseBodyAs(ErrorMessageResponse.class);
            if (error != null && error.message() != null && !error.message().isBlank()) {
                HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
                if (status == HttpStatus.BAD_REQUEST || status == HttpStatus.CONFLICT) {
                    return toUserFriendlyError(error.message(), defaultMessage);
                }
            }
        } catch (Exception ignored) {
            log.debug("Failed to parse error response", ignored);
        }
        return defaultMessage;
    }

    private String toUserFriendlyError(String rawMessage, String defaultMessage) {
        String message = rawMessage == null ? "" : rawMessage.toLowerCase(Locale.ROOT);
        if (message.contains("booking already inactive") || message.contains("payment already inactive")) {
            return botTextFactory.buildBookingAlreadyInactiveMessage();
        }
        if (message.contains("validation error")
                || message.contains("must be")
                || message.contains("invalid")
                || message.contains("failed to read request")) {
            if (message.contains("guest") || message.contains("adult") || message.contains("child")) {
                return maxGuestsMessage();
            }
            if (message.contains("date") || message.contains("check-in") || message.contains("check-out")) {
                return botTextFactory.buildValidationDateErrorMessage();
            }
            if (message.contains("price") || message.contains("area")) {
                return botTextFactory.buildValidationFilterErrorMessage();
            }
            return botTextFactory.buildValidationDefaultErrorMessage();
        }
        if (message.contains("no available rooms")) {
            return botTextFactory.buildNoRoomsAvailableForDatesMessage();
        }
        if (message.contains("promo")) {
            return botTextFactory.buildPromoCodeIgnoredMessage();
        }
        return rawMessage == null || rawMessage.isBlank() ? defaultMessage : rawMessage;
    }

    private String buildServiceUnavailableMessage(ResourceAccessException exception) {
        String message = exception.getMessage() == null ? "" : exception.getMessage().toLowerCase(Locale.ROOT);
        if (isClientError(message, "booking-service", bookingClientProperties.baseUrl())) {
            return botTextFactory.buildBookingServiceUnavailableMessage();
        }
        if (isClientError(message, "catalog-service", catalogClientProperties.baseUrl())) {
            return botTextFactory.buildCatalogServiceUnavailableMessage();
        }
        if (isClientError(message, "payment-service", paymentClientProperties.baseUrl())) {
            return botTextFactory.buildPaymentServiceUnavailableMessage();
        }
        if (isClientError(message, "notification-service", notificationClientProperties.baseUrl())) {
            return botTextFactory.buildNotificationServiceUnavailableMessage();
        }
        if (isClientError(message, "user-service", userClientProperties.baseUrl())) {
            return botTextFactory.buildUserServiceUnavailableMessage();
        }
        return botTextFactory.buildDefaultServiceUnavailableMessage();
    }

    private boolean isClientError(String message, String serviceName, String baseUrl) {
        return message.contains(serviceName)
                || (baseUrl != null && !baseUrl.isBlank() && message.contains(baseUrl.toLowerCase(Locale.ROOT)));
    }

    private boolean isPhotoMessage(Update update) {
        if (!update.hasCallbackQuery() || update.getCallbackQuery().getMessage() == null) {
            return false;
        }

        return update.getCallbackQuery().getMessage() instanceof Message message
                && message.hasPhoto()
                && message.getPhoto() != null
                && !message.getPhoto().isEmpty();
    }

    private String maxGuestsMessage() {
        return botTextFactory.buildMaxGuestsMessage(
                botFlowProperties.booking().maxGuests(),
                botFlowProperties.booking().maxAdults(),
                botFlowProperties.booking().maxChildren()
        );
    }

    private int parseSuffixInt(String data, String prefix) {
        return Integer.parseInt(data.substring(prefix.length()));
    }

    private void deletePhotoMessageIfNeeded(CallbackContext context) {
        if (context.photoMessage()) {
            botMessageService.deleteMessage(context.chatId(), context.messageId());
        }
    }

}
