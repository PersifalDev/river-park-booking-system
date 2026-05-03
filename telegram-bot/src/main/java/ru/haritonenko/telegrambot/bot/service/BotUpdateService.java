package ru.haritonenko.telegrambot.bot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientResponseException;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.haritonenko.commonlibs.dto.category.RoomCategoryResponseDto;
import ru.haritonenko.commonlibs.dto.category.RoomCategorySearchRequestDto;
import ru.haritonenko.commonlibs.dto.category.type.RoomType;
import ru.haritonenko.commonlibs.dto.error.ErrorMessageResponse;
import ru.haritonenko.commonlibs.dto.photo.RoomCategoryPhotoResponseDto;
import ru.haritonenko.commonlibs.dto.service.ServiceItemResponseDto;
import ru.haritonenko.commonlibs.utils.pages.PageResponse;
import ru.haritonenko.telegrambot.bot.state.AvailableRoomSearchDraft;
import ru.haritonenko.telegrambot.bot.state.BookingDraft;
import ru.haritonenko.telegrambot.bot.state.ChatStateService;
import ru.haritonenko.telegrambot.bot.state.ChatStateType;
import ru.haritonenko.telegrambot.bot.util.BotKeyboardFactory;
import ru.haritonenko.telegrambot.bot.util.BotTextFactory;
import ru.haritonenko.telegrambot.client.BookingClient;
import ru.haritonenko.telegrambot.client.CatalogClient;
import ru.haritonenko.telegrambot.client.NotificationClient;
import ru.haritonenko.telegrambot.client.PaymentClient;
import ru.haritonenko.telegrambot.config.BotProperties;
import ru.haritonenko.telegrambot.dto.booking.BotAvailableRoomSearchRequestDto;
import ru.haritonenko.telegrambot.dto.booking.BotBookingListItem;
import ru.haritonenko.telegrambot.dto.booking.BotBookingRequestDto;
import ru.haritonenko.telegrambot.dto.booking.BotBookingResponseDto;
import ru.haritonenko.telegrambot.dto.notification.BotNotificationResponseDto;
import ru.haritonenko.telegrambot.dto.payment.BotPaymentResponseDto;
import ru.haritonenko.telegrambot.service.BotMessageService;
import ru.haritonenko.telegrambot.service.auth.BotAuthService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
public class BotUpdateService {

    private static final int ROOM_PAGE_SIZE = 1;
    private static final int SERVICE_PAGE_SIZE = 4;
    private static final int PHOTO_PAGE_SIZE = 20;
    private static final int BOOKINGS_PAGE_SIZE = 50;
    private static final int NOTIFICATIONS_PAGE_SIZE = 5;
    private static final DateTimeFormatter HUMAN_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");

    private final CatalogClient catalogClient;
    private final BookingClient bookingClient;
    private final PaymentClient paymentClient;
    private final NotificationClient notificationClient;
    private final BotAuthService botAuthService;
    private final BotMessageService botMessageService;
    private final BotKeyboardFactory botKeyboardFactory;
    private final BotTextFactory botTextFactory;
    private final ChatStateService chatStateService;
    private final BotProperties botProperties;

    private final Map<Long, List<RoomCategoryPhotoResponseDto>> photoCache = new ConcurrentHashMap<>();

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
        } catch (Exception exception) {
            log.error("Unexpected bot error", exception);
            botMessageService.sendText(resolveChatId(update), botTextFactory.buildUnexpectedErrorMessage(), botKeyboardFactory.mainMenu());
        }
    }

    private boolean handleMenuAction(Long chatId, String text) {
        if ("Подобрать номер".equalsIgnoreCase(text)) {
            startRoomFilter(chatId);
            return true;
        }

        if ("Все номера".equalsIgnoreCase(text)) {
            chatStateService.reset(chatId);
            sendRoomsPage(chatId, 0, null, null);
            return true;
        }

        if ("Мои брони".equalsIgnoreCase(text)) {
            chatStateService.reset(chatId);
            sendBookings(chatId, null, false);
            return true;
        }

        if ("Уведомления".equalsIgnoreCase(text)) {
            chatStateService.reset(chatId);
            botMessageService.sendText(chatId, "Уведомления приходят автоматически. Я сам пришлю сообщение, когда статус брони изменится.", botKeyboardFactory.mainMenu());
            pushUnreadNotifications(chatId, false);
            return true;
        }

        if ("Найти номер".equalsIgnoreCase(text)) {
            chatStateService.reset(chatId);
            chatStateService.setType(chatId, ChatStateType.WAITING_ROOM_ID);
            List<RoomCategoryResponseDto> rooms = catalogClient.getRooms(0, 20).content();
            botMessageService.sendText(chatId, botTextFactory.buildRoomSelectionMessage(rooms), botKeyboardFactory.mainMenu());
            return true;
        }

        if ("Услуги".equalsIgnoreCase(text)) {
            chatStateService.reset(chatId);
            sendServicesPage(chatId, 0, null);
            return true;
        }

        if ("Найти услугу".equalsIgnoreCase(text)) {
            chatStateService.reset(chatId);
            chatStateService.setType(chatId, ChatStateType.WAITING_SERVICE_ID);
            List<ServiceItemResponseDto> services = catalogClient.getServices(0, 30);
            botMessageService.sendText(chatId, botTextFactory.buildServicePrompt(services), botKeyboardFactory.mainMenu());
            return true;
        }

        if ("Правила проживания".equalsIgnoreCase(text)) {
            sendRules(chatId);
            return true;
        }

        if ("Контакты".equalsIgnoreCase(text)) {
            botMessageService.sendText(chatId, botTextFactory.buildContactsMessage(botProperties.adminContact()), botKeyboardFactory.mainMenu());
            return true;
        }

        if ("Сайт".equalsIgnoreCase(text)) {
            botMessageService.sendText(chatId, botTextFactory.buildSiteMessage(), botKeyboardFactory.mainMenu());
            return true;
        }

        return false;
    }

    private void handleStateInput(Long chatId, String text) {
        ChatStateType stateType = chatStateService.get(chatId).type();

        switch (stateType) {
            case WAITING_ROOM_ID -> handleRoomId(chatId, text);
            case WAITING_SERVICE_ID -> handleServiceId(chatId, text);
            case WAITING_FILTER_GUESTS -> handleGuests(chatId, text);
            case WAITING_FILTER_CHECK_IN -> handleFilterCheckIn(chatId, text);
            case WAITING_FILTER_CHECK_OUT -> handleFilterCheckOut(chatId, text);
            case WAITING_FILTER_ROOM_TYPE -> handleRoomType(chatId, text);
            case WAITING_FILTER_PRICE_FROM -> handlePriceFrom(chatId, text);
            case WAITING_FILTER_PRICE_TO -> handlePriceTo(chatId, text);
            case WAITING_FILTER_MIN_AREA -> handleMinArea(chatId, text);
            case WAITING_BOOKING_CHECK_IN -> handleBookingCheckIn(chatId, text);
            case WAITING_BOOKING_CHECK_OUT -> handleBookingCheckOut(chatId, text);
            case WAITING_BOOKING_ADULTS -> handleBookingAdults(chatId, text);
            case WAITING_BOOKING_CHILDREN -> handleBookingChildren(chatId, text);
            case WAITING_BOOKING_PROMO -> handleBookingPromo(chatId, text);
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

        if ("noop".equals(data)) {
            return;
        }

        if ("menu:main".equals(data)) {
            chatStateService.reset(chatId);
            if (photoMessage) {
                botMessageService.deleteMessage(chatId, messageId);
                botMessageService.sendText(chatId, botTextFactory.buildMenuMessage(), botKeyboardFactory.mainMenu());
                return;
            }
            botMessageService.editText(chatId, messageId, botTextFactory.buildMenuMessage(), botKeyboardFactory.inlineMainMenu());
            return;
        }

        if ("menu:pick-room".equals(data)) {
            if (photoMessage) {
                botMessageService.deleteMessage(chatId, messageId);
            }
            startRoomFilter(chatId);
            return;
        }

        if ("menu:all-rooms".equals(data)) {
            chatStateService.reset(chatId);
            if (photoMessage) {
                botMessageService.deleteMessage(chatId, messageId);
                sendRoomsPage(chatId, 0, null, null);
                return;
            }
            sendRoomsPage(chatId, 0, messageId, null);
            return;
        }

        if ("menu:my-bookings".equals(data)) {
            if (photoMessage) {
                botMessageService.deleteMessage(chatId, messageId);
            }
            sendBookings(chatId, photoMessage ? null : messageId, false);
            return;
        }

        if ("menu:notifications".equals(data)) {
            if (photoMessage) {
                botMessageService.deleteMessage(chatId, messageId);
            }
            pushUnreadNotifications(chatId, true);
            return;
        }

        if ("menu:services".equals(data)) {
            chatStateService.reset(chatId);
            if (photoMessage) {
                botMessageService.deleteMessage(chatId, messageId);
                sendServicesPage(chatId, 0, null);
                return;
            }
            sendServicesPage(chatId, 0, messageId);
            return;
        }

        if ("menu:rules".equals(data)) {
            chatStateService.reset(chatId);
            if (photoMessage) {
                botMessageService.deleteMessage(chatId, messageId);
                botMessageService.sendText(chatId, botTextFactory.buildRulesMessage(catalogClient.getRuleDocument()), botKeyboardFactory.rulesKeyboard());
                return;
            }
            botMessageService.editText(chatId, messageId, botTextFactory.buildRulesMessage(catalogClient.getRuleDocument()), botKeyboardFactory.rulesKeyboard());
            return;
        }

        if ("menu:contacts".equals(data)) {
            chatStateService.reset(chatId);
            if (photoMessage) {
                botMessageService.deleteMessage(chatId, messageId);
                botMessageService.sendText(chatId, botTextFactory.buildContactsMessage(botProperties.adminContact()), botKeyboardFactory.mainMenu());
                return;
            }
            botMessageService.editText(chatId, messageId, botTextFactory.buildContactsMessage(botProperties.adminContact()), botKeyboardFactory.inlineMainMenu());
            return;
        }

        if (data.startsWith("filter:room-type:")) {
            handleRoomTypeSelection(chatId, messageId, data, photoMessage);
            return;
        }

        if (data.startsWith("rooms:page:")) {
            int pageNumber = Integer.parseInt(data.substring("rooms:page:".length()));
            RoomCategorySearchRequestDto filter = isFilterEmpty(chatStateService.get(chatId).filter())
                    ? null
                    : chatStateService.get(chatId).filter();

            if (photoMessage) {
                sendRoomsPage(chatId, pageNumber, messageId, filter);
                return;
            }

            botMessageService.deleteMessage(chatId, messageId);
            sendRoomsPage(chatId, pageNumber, null, filter);
            return;
        }

        if (data.startsWith("room:view:")) {
            handleRoomViewCallback(chatId, messageId, data, photoMessage);
            return;
        }

        if (data.startsWith("room:photos:")) {
            String[] parts = data.split(":");
            Long roomId = Long.parseLong(parts[2]);
            int roomPageNumber = Integer.parseInt(parts[3]);
            openPhotoGallery(chatId, roomId, roomPageNumber, messageId, photoMessage);
            return;
        }

        if (data.startsWith("room:photo:index:")) {
            handlePhotoIndexCallback(chatId, messageId, data);
            return;
        }

        if (data.startsWith("service:view:")) {
            handleServiceViewCallback(chatId, messageId, data);
            return;
        }

        if (data.startsWith("services:page:")) {
            int pageNumber = Integer.parseInt(data.substring("services:page:".length()));
            if (photoMessage) {
                botMessageService.deleteMessage(chatId, messageId);
                sendServicesPage(chatId, pageNumber, null);
                return;
            }
            sendServicesPage(chatId, pageNumber, messageId);
            return;
        }

        if (data.startsWith("booking:start:")) {
            Long roomId = Long.parseLong(data.substring("booking:start:".length()));
            startBookingFlow(chatId, roomId);
            return;
        }

        if (data.startsWith("booking:view:")) {
            UUID bookingId = UUID.fromString(data.substring("booking:view:".length()));
            showBookingDetails(chatId, bookingId, messageId, photoMessage);
            return;
        }

        if (data.startsWith("booking:cancel:")) {
            UUID bookingId = UUID.fromString(data.substring("booking:cancel:".length()));
            cancelBooking(chatId, bookingId, messageId, photoMessage);
            return;
        }

        if (data.startsWith("payment:confirm:")) {
            UUID bookingId = UUID.fromString(data.substring("payment:confirm:".length()));
            confirmPayment(chatId, bookingId, messageId, photoMessage);
            return;
        }

        if (data.startsWith("payment:cancel:")) {
            UUID bookingId = UUID.fromString(data.substring("payment:cancel:".length()));
            cancelPayment(chatId, bookingId, messageId, photoMessage);
            return;
        }

        if (data.startsWith("notification:read:")) {
            UUID notificationId = UUID.fromString(data.substring("notification:read:".length()));
            markNotificationRead(chatId, notificationId, messageId, photoMessage);
            return;
        }

        if ("notification:read-all".equals(data)) {
            markAllNotificationsRead(chatId, messageId, photoMessage);
            return;
        }

        if ("rules:file".equals(data)) {
            sendRuleFile(chatId);
        }
    }

    private void startRoomFilter(Long chatId) {
        chatStateService.reset(chatId);
        chatStateService.setType(chatId, ChatStateType.WAITING_FILTER_GUESTS);
        botMessageService.sendText(chatId, botTextFactory.buildFilterStartMessage(), botKeyboardFactory.mainMenu());
    }

    private void handleRoomTypeSelection(Long chatId, Integer messageId, String data, boolean photoMessage) {
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

    private void handleRoomViewCallback(Long chatId, Integer messageId, String data, boolean photoMessage) {
        String[] parts = data.split(":");
        Long roomId = Long.parseLong(parts[2]);
        int pageNumber = parts.length > 3 ? Integer.parseInt(parts[3]) : 0;
        RoomCategoryResponseDto room = catalogClient.getRoomById(roomId);

        if (photoMessage) {
            botMessageService.deleteMessage(chatId, messageId);
            botMessageService.sendText(chatId, botTextFactory.buildRoomDetails(room), botKeyboardFactory.roomDetails(roomId, pageNumber));
            return;
        }

        botMessageService.editText(chatId, messageId, botTextFactory.buildRoomDetails(room), botKeyboardFactory.roomDetails(roomId, pageNumber));
    }

    private void handlePhotoIndexCallback(Long chatId, Integer messageId, String data) {
        String[] parts = data.split(":");
        Long roomId = Long.parseLong(parts[3]);
        int photoIndex = Integer.parseInt(parts[4]);
        int roomPageNumber = Integer.parseInt(parts[5]);
        showPhotoPage(chatId, messageId, roomId, photoIndex, roomPageNumber);
    }

    private void handleServiceViewCallback(Long chatId, Integer messageId, String data) {
        String[] parts = data.split(":");
        Long serviceId = Long.parseLong(parts[2]);
        int pageNumber = Integer.parseInt(parts[3]);
        ServiceItemResponseDto service = catalogClient.getServiceById(serviceId);
        if (service.photoUrl() != null && !service.photoUrl().isBlank()) {
            if (!botMessageService.editPhoto(chatId, messageId, service.photoUrl(), botTextFactory.buildServiceDetailsCaption(service), botKeyboardFactory.serviceDetails(serviceId, pageNumber))) {
                botMessageService.deleteMessage(chatId, messageId);
                botMessageService.sendPhoto(chatId, service.photoUrl(), botTextFactory.buildServiceDetailsCaption(service), botKeyboardFactory.serviceDetails(serviceId, pageNumber));
            }
            return;
        }

        botMessageService.editText(chatId, messageId, botTextFactory.buildServiceDetails(service), botKeyboardFactory.serviceDetails(serviceId, pageNumber));
    }

    private void handleRoomId(Long chatId, String text) {
        Long roomId = parseLong(text, "Введите номер категории числом.", chatId);
        if (roomId == null) {
            return;
        }

        RoomCategoryResponseDto room = catalogClient.getRoomById(roomId);
        chatStateService.reset(chatId);
        botMessageService.sendText(chatId, botTextFactory.buildRoomDetails(room), botKeyboardFactory.roomDetails(roomId, 0));
    }

    private void handleServiceId(Long chatId, String text) {
        Long serviceId = parseLong(text, "Введите номер услуги числом.", chatId);
        if (serviceId == null) {
            return;
        }

        ServiceItemResponseDto service = catalogClient.getServiceById(serviceId);
        chatStateService.reset(chatId);
        if (service.photoUrl() != null && !service.photoUrl().isBlank()) {
            botMessageService.sendPhoto(chatId, service.photoUrl(), botTextFactory.buildServiceDetailsCaption(service), botKeyboardFactory.serviceDetails(serviceId, 0));
            return;
        }
        botMessageService.sendText(chatId, botTextFactory.buildServiceDetails(service), botKeyboardFactory.serviceDetails(serviceId, 0));
    }

    private void handleGuests(Long chatId, String text) {
        Integer guests = parsePositiveInteger(text, "Количество гостей должно быть целым числом.", chatId, botTextFactory.buildPositiveGuestsMessage());
        if (guests == null) {
            return;
        }

        AvailableRoomSearchDraft draft = chatStateService.get(chatId).availableRoomSearchDraft().toBuilder()
                .guests(guests)
                .build();
        chatStateService.updateAvailableRoomSearchDraft(chatId, draft);
        chatStateService.setType(chatId, ChatStateType.WAITING_FILTER_CHECK_IN);
        botMessageService.sendText(chatId, botTextFactory.buildAskFilterCheckInMessage(), botKeyboardFactory.mainMenu());
    }

    private void handleFilterCheckIn(Long chatId, String text) {
        LocalDate checkInDate = parseDate(text, chatId);
        if (checkInDate == null) {
            return;
        }
        if (checkInDate.isBefore(LocalDate.now())) {
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

    private void handleFilterCheckOut(Long chatId, String text) {
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
        chatStateService.setType(chatId, ChatStateType.WAITING_FILTER_ROOM_TYPE);
        botMessageService.sendText(chatId, botTextFactory.buildAskRoomTypeMessage(), botKeyboardFactory.roomTypeSelection());
    }

    private void handleRoomType(Long chatId, String text) {
        AvailableRoomSearchDraft.AvailableRoomSearchDraftBuilder builder = chatStateService.get(chatId).availableRoomSearchDraft().toBuilder();

        if (!"-".equals(text.trim())) {
            try {
                builder.roomType(parseRoomType(text));
            } catch (IllegalArgumentException exception) {
                botMessageService.sendText(chatId, "Не удалось распознать категорию. Выберите её кнопками ниже или отправьте -", botKeyboardFactory.roomTypeSelection());
                return;
            }
        } else {
            builder.roomType(null);
        }

        chatStateService.updateAvailableRoomSearchDraft(chatId, builder.build());
        chatStateService.setType(chatId, ChatStateType.WAITING_FILTER_PRICE_FROM);
        botMessageService.sendText(chatId, botTextFactory.buildAskPriceFromMessage(), botKeyboardFactory.mainMenu());
    }

    private void handlePriceFrom(Long chatId, String text) {
        BigDecimal value = parseOptionalNonNegativeDecimal(text, "Минимальная цена должна быть числом или -", chatId);
        if (value == null && !"-".equals(text.trim())) {
            return;
        }

        AvailableRoomSearchDraft draft = chatStateService.get(chatId).availableRoomSearchDraft().toBuilder()
                .priceFrom(value)
                .build();
        chatStateService.updateAvailableRoomSearchDraft(chatId, draft);
        chatStateService.setType(chatId, ChatStateType.WAITING_FILTER_PRICE_TO);
        botMessageService.sendText(chatId, botTextFactory.buildAskPriceToMessage(), botKeyboardFactory.mainMenu());
    }

    private void handlePriceTo(Long chatId, String text) {
        BigDecimal value = parseOptionalNonNegativeDecimal(text, "Максимальная цена должна быть числом или -", chatId);
        if (value == null && !"-".equals(text.trim())) {
            return;
        }

        AvailableRoomSearchDraft draft = chatStateService.get(chatId).availableRoomSearchDraft().toBuilder()
                .priceTo(value)
                .build();
        chatStateService.updateAvailableRoomSearchDraft(chatId, draft);
        chatStateService.setType(chatId, ChatStateType.WAITING_FILTER_MIN_AREA);
        botMessageService.sendText(chatId, botTextFactory.buildAskMinAreaMessage(), botKeyboardFactory.mainMenu());
    }

    private void handleMinArea(Long chatId, String text) {
        BigDecimal value = parseOptionalNonNegativeDecimal(text, "Минимальная площадь должна быть числом или -", chatId);
        if (value == null && !"-".equals(text.trim())) {
            return;
        }

        AvailableRoomSearchDraft draft = chatStateService.get(chatId).availableRoomSearchDraft().toBuilder()
                .minArea(value)
                .build();
        chatStateService.updateAvailableRoomSearchDraft(chatId, draft);
        chatStateService.setType(chatId, ChatStateType.IDLE);
        sendRoomsPage(chatId, 0, null, null);
    }

    private void startBookingFlow(Long chatId, Long roomId) {
        RoomCategoryResponseDto room = catalogClient.getRoomById(roomId);
        chatStateService.reset(chatId);
        chatStateService.updateBookingDraft(chatId, BookingDraft.builder().roomCategoryId(roomId).build());
        chatStateService.setType(chatId, ChatStateType.WAITING_BOOKING_CHECK_IN);
        botMessageService.sendText(chatId, botTextFactory.buildBookingStartMessage(room), botKeyboardFactory.mainMenu());
    }

    private void handleBookingCheckIn(Long chatId, String text) {
        LocalDate checkInDate = parseDate(text, chatId);
        if (checkInDate == null) {
            return;
        }
        if (checkInDate.isBefore(LocalDate.now())) {
            botMessageService.sendText(chatId, botTextFactory.buildPastDateMessage(), botKeyboardFactory.mainMenu());
            return;
        }

        BookingDraft bookingDraft = chatStateService.get(chatId).bookingDraft().toBuilder()
                .checkInDate(checkInDate)
                .build();
        chatStateService.updateBookingDraft(chatId, bookingDraft);
        chatStateService.setType(chatId, ChatStateType.WAITING_BOOKING_CHECK_OUT);
        botMessageService.sendText(chatId, botTextFactory.buildAskBookingCheckOutMessage(checkInDate), botKeyboardFactory.mainMenu());
    }

    private void handleBookingCheckOut(Long chatId, String text) {
        LocalDate checkOutDate = parseDate(text, chatId);
        if (checkOutDate == null) {
            return;
        }
        if (checkOutDate.isBefore(LocalDate.now())) {
            botMessageService.sendText(chatId, botTextFactory.buildPastDateMessage(), botKeyboardFactory.mainMenu());
            return;
        }

        BookingDraft currentDraft = chatStateService.get(chatId).bookingDraft();
        if (currentDraft.checkInDate() == null || !checkOutDate.isAfter(currentDraft.checkInDate())) {
            botMessageService.sendText(chatId, botTextFactory.buildCheckoutBeforeCheckinMessage(), botKeyboardFactory.mainMenu());
            return;
        }

        BookingDraft bookingDraft = currentDraft.toBuilder()
                .checkOutDate(checkOutDate)
                .build();
        chatStateService.updateBookingDraft(chatId, bookingDraft);
        chatStateService.setType(chatId, ChatStateType.WAITING_BOOKING_ADULTS);
        botMessageService.sendText(chatId, botTextFactory.buildAskBookingAdultsMessage(bookingDraft.checkInDate(), checkOutDate), botKeyboardFactory.mainMenu());
    }

    private void handleBookingAdults(Long chatId, String text) {
        Integer adults = parsePositiveInteger(text, "Количество взрослых должно быть целым числом.", chatId, botTextFactory.buildPositiveAdultsMessage());
        if (adults == null) {
            return;
        }

        BookingDraft bookingDraft = chatStateService.get(chatId).bookingDraft().toBuilder()
                .adultCount(adults)
                .build();
        chatStateService.updateBookingDraft(chatId, bookingDraft);
        chatStateService.setType(chatId, ChatStateType.WAITING_BOOKING_CHILDREN);
        botMessageService.sendText(chatId, botTextFactory.buildAskBookingChildrenMessage(adults), botKeyboardFactory.mainMenu());
    }

    private void handleBookingChildren(Long chatId, String text) {
        Integer children = parseNonNegativeInteger(text, "Количество детей должно быть целым числом.", chatId, botTextFactory.buildChildrenCountMessage());
        if (children == null) {
            return;
        }

        BookingDraft currentDraft = chatStateService.get(chatId).bookingDraft();
        RoomCategoryResponseDto room = catalogClient.getRoomById(currentDraft.roomCategoryId());
        int totalGuests = safeInt(currentDraft.adultCount()) + children;
        if (room.maxGuests() != null && totalGuests > room.maxGuests()) {
            botMessageService.sendText(chatId, botTextFactory.buildGuestOverflowMessage(room), botKeyboardFactory.mainMenu());
            return;
        }

        BookingDraft bookingDraft = currentDraft.toBuilder()
                .childrenCount(children)
                .build();
        chatStateService.updateBookingDraft(chatId, bookingDraft);
        chatStateService.setType(chatId, ChatStateType.WAITING_BOOKING_PROMO);
        botMessageService.sendText(chatId, botTextFactory.buildAskBookingPromoMessage(bookingDraft.adultCount(), children), botKeyboardFactory.mainMenu());
    }

    private void handleBookingPromo(Long chatId, String text) {
        String promoCode = "-".equals(text.trim()) ? null : text.trim();
        BookingDraft bookingDraft = chatStateService.get(chatId).bookingDraft().toBuilder()
                .promoCode(promoCode)
                .build();
        chatStateService.updateBookingDraft(chatId, bookingDraft);
        createBooking(chatId);
    }

    private void createBooking(Long chatId) {
        String jwt = botAuthService.getJwt(chatId);
        BookingDraft bookingDraft = chatStateService.get(chatId).bookingDraft();
        int totalGuests = safeInt(bookingDraft.adultCount()) + safeInt(bookingDraft.childrenCount());

        RoomCategoryResponseDto room = catalogClient.getRoomById(bookingDraft.roomCategoryId());
        if (room.maxGuests() != null && totalGuests > room.maxGuests()) {
            botMessageService.sendText(chatId, botTextFactory.buildGuestOverflowMessage(room), botKeyboardFactory.mainMenu());
            return;
        }

        botMessageService.sendText(chatId, botTextFactory.buildBookingCreatingMessage(), botKeyboardFactory.mainMenu());

        BotBookingResponseDto createdBooking = bookingClient.createBooking(jwt, new BotBookingRequestDto(
                bookingDraft.roomCategoryId(),
                bookingDraft.checkInDate(),
                bookingDraft.checkOutDate(),
                totalGuests,
                bookingDraft.adultCount(),
                bookingDraft.childrenCount(),
                bookingDraft.promoCode()
        ));

        BotBookingResponseDto actualBooking = awaitBookingResolution(jwt, createdBooking.id());
        BotPaymentResponseDto payment = null;
        if (actualBooking != null && ("HOLD".equalsIgnoreCase(actualBooking.status()) || "CONFIRMED".equalsIgnoreCase(actualBooking.status()))) {
            payment = awaitPaymentResolution(jwt, actualBooking.id());
        }

        chatStateService.reset(chatId);

        if (actualBooking == null) {
            botMessageService.sendText(chatId, botTextFactory.buildUnexpectedErrorMessage(), botKeyboardFactory.mainMenu());
            return;
        }

        if (List.of("FAILED", "EXPIRED", "CANCELLED").contains(actualBooking.status() == null ? "" : actualBooking.status().toUpperCase(Locale.ROOT))) {
            botMessageService.sendText(chatId, botTextFactory.buildBookingFailedMessage(actualBooking), botKeyboardFactory.mainMenu());
            pushUnreadNotifications(chatId, true);
            return;
        }

        if ("CREATED".equalsIgnoreCase(actualBooking.status())) {
            botMessageService.sendText(chatId, botTextFactory.buildBookingProcessingMessage(actualBooking), botKeyboardFactory.mainMenu());
            return;
        }

        botMessageService.sendText(
                chatId,
                botTextFactory.buildBookingCreatedMessage(actualBooking, room, payment, botProperties.adminContact()),
                botKeyboardFactory.bookingDetails(actualBooking, payment)
        );
        pushUnreadNotifications(chatId, false);
    }

    private void sendRoomsPage(Long chatId, int pageNumber, Integer messageId, RoomCategorySearchRequestDto filter) {
        AvailableRoomSearchDraft draft = chatStateService.get(chatId).availableRoomSearchDraft();
        boolean hasDateFilter = draft != null && draft.checkInDate() != null && draft.checkOutDate() != null;

        PageResponse<RoomCategoryResponseDto> page;
        if (hasDateFilter) {
            page = bookingClient.searchAvailableRooms(
                    botAuthService.getJwt(chatId),
                    new BotAvailableRoomSearchRequestDto(
                            draft.checkInDate(),
                            draft.checkOutDate(),
                            draft.guests(),
                            draft.roomType(),
                            draft.priceFrom(),
                            draft.priceTo(),
                            draft.minArea()
                    ),
                    pageNumber,
                    ROOM_PAGE_SIZE
            );
        } else {
            page = filter == null
                    ? catalogClient.getRooms(pageNumber, ROOM_PAGE_SIZE)
                    : catalogClient.searchRooms(filter, pageNumber, ROOM_PAGE_SIZE);
        }

        if (page.content() == null || page.content().isEmpty()) {
            botMessageService.sendText(chatId, botTextFactory.buildNoRoomsFoundMessage(), botKeyboardFactory.mainMenu());
            return;
        }

        RoomCategoryResponseDto room = page.content().getFirst();
        String caption = botTextFactory.buildRoomCard(room, page.number(), page.totalPages(), hasDateFilter || filter != null);

        if (messageId == null) {
            botMessageService.sendPhoto(chatId, room.mainPhotoUrl(), caption, botKeyboardFactory.roomCard(room.id(), page.number(), page.totalPages()));
            return;
        }

        if (!botMessageService.editPhoto(chatId, messageId, room.mainPhotoUrl(), caption, botKeyboardFactory.roomCard(room.id(), page.number(), page.totalPages()))) {
            botMessageService.deleteMessage(chatId, messageId);
            botMessageService.sendPhoto(chatId, room.mainPhotoUrl(), caption, botKeyboardFactory.roomCard(room.id(), page.number(), page.totalPages()));
        }
    }

    private void sendServicesPage(Long chatId, int pageNumber, Integer messageId) {
        List<ServiceItemResponseDto> services = catalogClient.getServices(pageNumber, SERVICE_PAGE_SIZE);
        List<ServiceItemResponseDto> nextServices = catalogClient.getServices(pageNumber + 1, SERVICE_PAGE_SIZE);
        String text = botTextFactory.buildServicesMessage(services, pageNumber);

        if (messageId == null) {
            botMessageService.sendText(chatId, text, botKeyboardFactory.servicesPage(services, pageNumber, !nextServices.isEmpty()));
            return;
        }

        botMessageService.editText(chatId, messageId, text, botKeyboardFactory.servicesPage(services, pageNumber, !nextServices.isEmpty()));
    }

    private void sendBookings(Long chatId, Integer messageId, boolean photoMessage) {
        String jwt = botAuthService.getJwt(chatId);
        List<BotBookingResponseDto> bookings = bookingClient.getBookings(jwt, 0, BOOKINGS_PAGE_SIZE).stream()
                .filter(this::isVisibleBookingForUser)
                .sorted(java.util.Comparator.comparing(BotBookingResponseDto::createdAt, java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())).reversed())
                .toList();

        if (bookings.isEmpty()) {
            if (messageId != null && !photoMessage) {
                botMessageService.editText(chatId, messageId, botTextFactory.buildMyBookingsEmptyMessage(), botKeyboardFactory.inlineMainMenu());
                return;
            }
            botMessageService.sendText(chatId, botTextFactory.buildMyBookingsEmptyMessage(), botKeyboardFactory.mainMenu());
            return;
        }

        List<BotBookingListItem> bookingItems = buildBookingListItems(bookings);

        if (messageId != null && !photoMessage) {
            botMessageService.editText(chatId, messageId, botTextFactory.buildMyBookingsMessage(bookings), botKeyboardFactory.bookingsList(bookingItems));
            return;
        }
        botMessageService.sendText(chatId, botTextFactory.buildMyBookingsMessage(bookings), botKeyboardFactory.bookingsList(bookingItems));
    }

    private void showBookingDetails(Long chatId, UUID bookingId, Integer messageId, boolean photoMessage) {
        String jwt = botAuthService.getJwt(chatId);
        BotBookingResponseDto booking = bookingClient.getBooking(jwt, bookingId);
        RoomCategoryResponseDto room = catalogClient.getRoomById(booking.roomCategoryId());
        BotPaymentResponseDto payment = getPaymentSafely(jwt, bookingId);
        String text = botTextFactory.buildBookingDetails(booking, room, payment);

        if (photoMessage) {
            botMessageService.deleteMessage(chatId, messageId);
            botMessageService.sendText(chatId, text, botKeyboardFactory.bookingDetails(booking, payment));
            return;
        }

        botMessageService.editText(chatId, messageId, text, botKeyboardFactory.bookingDetails(booking, payment));
    }

    private void cancelBooking(Long chatId, UUID bookingId, Integer messageId, boolean photoMessage) {
        String jwt = botAuthService.getJwt(chatId);
        BotBookingResponseDto cancelledBooking = bookingClient.cancelBooking(jwt, bookingId);
        BotPaymentResponseDto payment = getPaymentSafely(jwt, bookingId);
        sendActionResult(chatId, messageId, photoMessage, botTextFactory.buildBookingCancelledMessage(cancelledBooking), botKeyboardFactory.bookingDetails(cancelledBooking, payment));
        pushUnreadNotifications(chatId, false);
    }

    private void confirmPayment(Long chatId, UUID bookingId, Integer messageId, boolean photoMessage) {
        String jwt = botAuthService.getJwt(chatId);
        paymentClient.confirmPayment(jwt, bookingId);
        BotBookingResponseDto booking;
        try {
            booking = bookingClient.confirmBooking(jwt, bookingId);
        } catch (RestClientResponseException exception) {
            booking = awaitBookingConfirmation(jwt, bookingId);
        }
        BotPaymentResponseDto payment = awaitPaymentResolution(jwt, bookingId);
        if (booking == null) {
            sendActionResult(chatId, messageId, photoMessage, botTextFactory.buildUnexpectedErrorMessage(), botKeyboardFactory.inlineMainMenu());
            return;
        }
        sendActionResult(chatId, messageId, photoMessage, botTextFactory.buildPaymentConfirmedMessage(booking), botKeyboardFactory.bookingDetails(booking, payment));
        pushUnreadNotifications(chatId, false);
    }

    private void cancelPayment(Long chatId, UUID bookingId, Integer messageId, boolean photoMessage) {
        String jwt = botAuthService.getJwt(chatId);
        paymentClient.cancelPayment(jwt, bookingId);
        BotBookingResponseDto booking = awaitBookingResolution(jwt, bookingId);
        BotPaymentResponseDto payment = getPaymentSafely(jwt, bookingId);
        if (booking == null) {
            sendActionResult(chatId, messageId, photoMessage, botTextFactory.buildUnexpectedErrorMessage(), botKeyboardFactory.inlineMainMenu());
            return;
        }
        sendActionResult(chatId, messageId, photoMessage, botTextFactory.buildBookingCancelledMessage(booking), botKeyboardFactory.bookingDetails(booking, payment));
        pushUnreadNotifications(chatId, false);
    }

    public void pushUnreadNotifications(Long chatId, boolean notifyIfEmpty) {
        String jwt = botAuthService.getJwt(chatId);
        List<BotNotificationResponseDto> notifications = notificationClient.getUnreadNotifications(jwt, 0, NOTIFICATIONS_PAGE_SIZE);
        if (notifications.isEmpty()) {
            if (notifyIfEmpty) {
                botMessageService.sendText(chatId, "Новых уведомлений нет.", botKeyboardFactory.mainMenu());
            }
            return;
        }
        for (BotNotificationResponseDto notification : notifications) {
            botMessageService.sendText(chatId, botTextFactory.buildNotificationMessage(notification), botKeyboardFactory.mainMenu());
            notificationClient.markAsRead(jwt, notification.id());
        }
    }

    private void markNotificationRead(Long chatId, UUID notificationId, Integer messageId, boolean photoMessage) {
        String jwt = botAuthService.getJwt(chatId);
        notificationClient.markAsRead(jwt, notificationId);
        if (photoMessage) {
            botMessageService.deleteMessage(chatId, messageId);
            botMessageService.sendText(chatId, "Уведомление отмечено как прочитанное.", botKeyboardFactory.mainMenu());
            return;
        }
        botMessageService.editText(chatId, messageId, "Уведомление отмечено как прочитанное.", botKeyboardFactory.inlineMainMenu());
    }

    private void markAllNotificationsRead(Long chatId, Integer messageId, boolean photoMessage) {
        String jwt = botAuthService.getJwt(chatId);
        notificationClient.markAllAsRead(jwt);
        if (photoMessage) {
            botMessageService.deleteMessage(chatId, messageId);
            botMessageService.sendText(chatId, "Все уведомления отмечены как прочитанные.", botKeyboardFactory.mainMenu());
            return;
        }
        botMessageService.editText(chatId, messageId, "Все уведомления отмечены как прочитанные.", botKeyboardFactory.inlineMainMenu());
    }

    private void sendRules(Long chatId) {
        botMessageService.sendText(chatId, botTextFactory.buildRulesMessage(catalogClient.getRuleDocument()), botKeyboardFactory.rulesKeyboard());
    }

    private void sendRuleFile(Long chatId) {
        var ruleDocument = catalogClient.getRuleDocument();
        botMessageService.sendDocument(
                chatId,
                ruleDocument == null || ruleDocument.fileName() == null ? "river-park-rules.pdf" : ruleDocument.fileName(),
                catalogClient.downloadRuleDocument()
        );
    }

    private void openPhotoGallery(Long chatId, Long roomId, int roomPageNumber, Integer existingMessageId, boolean photoMessage) {
        RoomCategoryResponseDto room = catalogClient.getRoomById(roomId);
        List<RoomCategoryPhotoResponseDto> photos = photoCache.computeIfAbsent(roomId, key -> catalogClient.getRoomPhotos(roomId, 0, PHOTO_PAGE_SIZE));

        if (photos.isEmpty()) {
            if (existingMessageId != null) {
                if (photoMessage) {
                    botMessageService.deleteMessage(chatId, existingMessageId);
                    botMessageService.sendText(chatId, botTextFactory.buildNoPhotosMessage(), botKeyboardFactory.mainMenu());
                    return;
                }

                botMessageService.editText(chatId, existingMessageId, botTextFactory.buildNoPhotosMessage(), botKeyboardFactory.inlineMainMenu());
                return;
            }

            botMessageService.sendText(chatId, botTextFactory.buildNoPhotosMessage(), botKeyboardFactory.mainMenu());
            return;
        }

        if (existingMessageId != null && photoMessage) {
            showPhotoPage(chatId, existingMessageId, roomId, 0, roomPageNumber);
            return;
        }

        if (existingMessageId != null) {
            botMessageService.deleteMessage(chatId, existingMessageId);
        }

        String caption = botTextFactory.buildPhotosCaption(room, 0, photos.size());
        botMessageService.sendPhoto(chatId, photos.getFirst().url(), caption, botKeyboardFactory.photoGallery(roomId, 0, photos.size(), roomPageNumber));
    }

    private void showPhotoPage(Long chatId, Integer messageId, Long roomId, int photoIndex, int roomPageNumber) {
        List<RoomCategoryPhotoResponseDto> photos = photoCache.computeIfAbsent(roomId, key -> catalogClient.getRoomPhotos(roomId, 0, PHOTO_PAGE_SIZE));
        if (photos.isEmpty() || photoIndex < 0 || photoIndex >= photos.size()) {
            log.warn("Skip photo page because index is invalid. roomId={}, photoIndex={}, size={}", roomId, photoIndex, photos.size());
            return;
        }

        RoomCategoryResponseDto room = catalogClient.getRoomById(roomId);
        String caption = botTextFactory.buildPhotosCaption(room, photoIndex, photos.size());

        if (!botMessageService.editPhoto(chatId, messageId, photos.get(photoIndex).url(), caption, botKeyboardFactory.photoGallery(roomId, photoIndex, photos.size(), roomPageNumber))) {
            botMessageService.deleteMessage(chatId, messageId);
            botMessageService.sendPhoto(chatId, photos.get(photoIndex).url(), caption, botKeyboardFactory.photoGallery(roomId, photoIndex, photos.size(), roomPageNumber));
        }
    }

    private BotBookingResponseDto awaitBookingResolution(String jwt, UUID bookingId) {
        BotBookingResponseDto lastBooking = null;
        for (int attempt = 0; attempt < 30; attempt++) {
            lastBooking = bookingClient.getBooking(jwt, bookingId);
            if (lastBooking != null && isResolvedBookingStatus(lastBooking.status())) {
                return lastBooking;
            }
            sleep(1500);
        }
        return lastBooking;
    }

    private BotBookingResponseDto awaitBookingConfirmation(String jwt, UUID bookingId) {
        BotBookingResponseDto lastBooking = null;
        for (int attempt = 0; attempt < 20; attempt++) {
            lastBooking = bookingClient.getBooking(jwt, bookingId);
            if (lastBooking != null && isConfirmedBookingStatus(lastBooking.status())) {
                return lastBooking;
            }
            sleep(1200);
        }
        return lastBooking;
    }

    private BotPaymentResponseDto awaitPaymentResolution(String jwt, UUID bookingId) {
        BotPaymentResponseDto lastPayment = null;
        for (int attempt = 0; attempt < 10; attempt++) {
            lastPayment = getPaymentSafely(jwt, bookingId);
            if (lastPayment != null) {
                return lastPayment;
            }
            sleep(1000);
        }
        return lastPayment;
    }

    private BotPaymentResponseDto getPaymentSafely(String jwt, UUID bookingId) {
        try {
            return paymentClient.getPaymentByBookingId(jwt, bookingId);
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode() == HttpStatus.NOT_FOUND) {
                return null;
            }
            throw exception;
        }
    }

    private boolean isVisibleBookingForUser(BotBookingResponseDto booking) {
        if (booking == null || booking.status() == null) {
            return false;
        }
        return List.of("CREATED", "HOLD", "CONFIRMED").contains(booking.status().toUpperCase(Locale.ROOT));
    }

    private boolean isResolvedBookingStatus(String status) {
        if (status == null) {
            return false;
        }
        return List.of("HOLD", "CONFIRMED", "FAILED", "EXPIRED", "CANCELLED").contains(status.toUpperCase(Locale.ROOT));
    }

    private boolean isConfirmedBookingStatus(String status) {
        if (status == null) {
            return false;
        }
        return List.of("CONFIRMED", "FAILED", "EXPIRED", "CANCELLED").contains(status.toUpperCase(Locale.ROOT));
    }


    private List<BotBookingListItem> buildBookingListItems(List<BotBookingResponseDto> bookings) {
        return java.util.stream.IntStream.range(0, bookings.size())
                .mapToObj(index -> {
                    BotBookingResponseDto booking = bookings.get(index);
                    String roomTitle = resolveRoomTitle(booking.roomCategoryId());
                    return new BotBookingListItem(
                            booking.id(),
                            botTextFactory.buildBookingListLabel(index + 1, roomTitle, booking)
                    );
                })
                .toList();
    }

    private String resolveRoomTitle(Long roomCategoryId) {
        if (roomCategoryId == null) {
            return "Номер";
        }
        try {
            RoomCategoryResponseDto room = catalogClient.getRoomById(roomCategoryId);
            return room == null || room.name() == null ? "Номер" : switch (room.name()) {
                case STANDARD -> "Standard";
                case STANDARD_DOUBLE -> "Standard Double";
                case STANDARD_PLUS -> "Standard Plus";
                case STUDIO -> "Studio";
                case BUSINESS_STUDIO -> "Business Studio";
                case ECONOMY -> "Economy";
            };
        } catch (Exception exception) {
            log.warn("Failed to resolve room title for categoryId={}", roomCategoryId, exception);
            return "Номер";
        }
    }

    private void sendActionResult(Long chatId, Integer messageId, boolean photoMessage, String text, org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup keyboard) {
        if (photoMessage) {
            botMessageService.deleteMessage(chatId, messageId);
            botMessageService.sendText(chatId, text, keyboard);
            return;
        }
        botMessageService.editText(chatId, messageId, text, keyboard);
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

    private Long parseLong(String text, String errorMessage, Long chatId) {
        try {
            long value = Long.parseLong(text.trim());
            if (value < 0) {
                botMessageService.sendText(chatId, botTextFactory.buildNegativeValueMessage(), botKeyboardFactory.mainMenu());
                return null;
            }
            return value;
        } catch (NumberFormatException exception) {
            botMessageService.sendText(chatId, errorMessage, botKeyboardFactory.mainMenu());
            return null;
        }
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

    private Integer parseNonNegativeInteger(String text, String errorMessage, Long chatId, String negativeMessage) {
        try {
            int value = Integer.parseInt(text.trim());
            if (value < 0) {
                botMessageService.sendText(chatId, negativeMessage, botKeyboardFactory.mainMenu());
                return null;
            }
            return value;
        } catch (NumberFormatException exception) {
            botMessageService.sendText(chatId, errorMessage, botKeyboardFactory.mainMenu());
            return null;
        }
    }

    private boolean isFilterEmpty(RoomCategorySearchRequestDto filter) {
        return filter == null
                || (filter.guests() == null
                && filter.roomType() == null
                && filter.priceFrom() == null
                && filter.priceTo() == null
                && filter.minArea() == null);
    }

    private BigDecimal parseOptionalNonNegativeDecimal(String text, String errorMessage, Long chatId) {
        if ("-".equals(text.trim())) {
            return null;
        }

        try {
            BigDecimal value = new BigDecimal(text.trim());
            if (value.signum() < 0) {
                botMessageService.sendText(chatId, botTextFactory.buildNegativeValueMessage(), botKeyboardFactory.mainMenu());
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
            if (value.matches("\\d{2}\\.\\d{2}\\.\\d{4}")) {
                return LocalDate.parse(value, HUMAN_DATE_FORMATTER);
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

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    private String extractErrorMessage(RestClientResponseException exception, String defaultMessage) {
        try {
            ErrorMessageResponse error = exception.getResponseBodyAs(ErrorMessageResponse.class);
            if (error != null && error.message() != null && !error.message().isBlank()) {
                HttpStatus status = HttpStatus.resolve(exception.getStatusCode().value());
                if (status == HttpStatus.BAD_REQUEST || status == HttpStatus.CONFLICT) {
                    return error.message();
                }
            }
        } catch (Exception ignored) {
            log.debug("Failed to parse error response", ignored);
        }
        return defaultMessage;
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
}
