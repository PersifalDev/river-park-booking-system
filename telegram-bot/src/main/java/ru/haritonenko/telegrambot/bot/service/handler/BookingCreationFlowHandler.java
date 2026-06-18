package ru.haritonenko.telegrambot.bot.service.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import ru.haritonenko.commonlibs.dto.category.RoomCategoryResponseDto;
import ru.haritonenko.telegrambot.bot.service.storage.BotConversationStore;
import ru.haritonenko.telegrambot.bot.state.AvailableRoomSearchDraft;
import ru.haritonenko.telegrambot.bot.state.BookingDraft;
import ru.haritonenko.telegrambot.bot.state.ChatStateService;
import ru.haritonenko.telegrambot.bot.state.ChatStateType;
import ru.haritonenko.telegrambot.bot.util.BotKeyboardFactory;
import ru.haritonenko.telegrambot.bot.util.BotTextFactory;
import ru.haritonenko.telegrambot.client.BookingClient;
import ru.haritonenko.telegrambot.client.CatalogClient;
import ru.haritonenko.telegrambot.client.PaymentClient;
import ru.haritonenko.telegrambot.config.BotFlowProperties;
import ru.haritonenko.telegrambot.config.BotProperties;
import ru.haritonenko.telegrambot.dto.booking.BotBookingRequestDto;
import ru.haritonenko.telegrambot.dto.booking.BotBookingResponseDto;
import ru.haritonenko.telegrambot.dto.booking.BotTariffResponseDto;
import ru.haritonenko.telegrambot.dto.payment.BotPaymentResponseDto;
import ru.haritonenko.telegrambot.service.BotMessageService;
import ru.haritonenko.telegrambot.service.auth.BotAuthService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class BookingCreationFlowHandler {

    private static final ZoneId NOVOSIBIRSK_ZONE = ZoneId.of("Asia/Novosibirsk");
    private static final DateTimeFormatter FLEXIBLE_HUMAN_DATE_FORMATTER = DateTimeFormatter.ofPattern("d.M.uuuu")
            .withResolverStyle(ResolverStyle.STRICT);

    private final CatalogClient catalogClient;
    private final BookingClient bookingClient;
    private final PaymentClient paymentClient;
    private final BotAuthService botAuthService;
    private final BotMessageService botMessageService;
    private final BotKeyboardFactory botKeyboardFactory;
    private final BotTextFactory botTextFactory;
    private final ChatStateService chatStateService;
    private final BotProperties botProperties;
    private final BotFlowProperties botFlowProperties;
    private final NotificationFlowHandler notificationFlowHandler;
    private final CatalogFlowHandler catalogFlowHandler;
    private final BotConversationStore conversationStore;

    public void startBookingFlow(Long chatId, Long roomId) {
        RoomCategoryResponseDto room = catalogFlowHandler.resolveRoomForDisplay(chatId, roomId);
        AvailableRoomSearchDraft searchDraft = chatStateService.get(chatId).availableRoomSearchDraft();
        chatStateService.reset(chatId);
        BookingDraft.BookingDraftBuilder draftBuilder = BookingDraft.builder().roomCategoryId(roomId);
        if (searchDraft != null && searchDraft.checkInDate() != null && searchDraft.checkOutDate() != null) {
            draftBuilder
                    .checkInDate(searchDraft.checkInDate())
                    .checkOutDate(searchDraft.checkOutDate());
        }
        BookingDraft bookingDraft = draftBuilder.build();
        chatStateService.updateBookingDraft(chatId, bookingDraft);
        if (bookingDraft.checkInDate() != null && bookingDraft.checkOutDate() != null) {
            chatStateService.setType(chatId, ChatStateType.WAITING_BOOKING_ADULTS);
            botMessageService.sendText(
                    chatId,
                    botTextFactory.buildBookingStartMessage(room)
                            + "\n\n"
                            + botTextFactory.buildBookingSelectedPeriodMessage(
                            bookingDraft.checkInDate(),
                            bookingDraft.checkOutDate(),
                            valueOrDash(availableUnits(room))
                    ),
                    botKeyboardFactory.mainMenu()
            );
            return;
        }
        chatStateService.setType(chatId, ChatStateType.WAITING_BOOKING_CHECK_IN);
        botMessageService.sendText(chatId, botTextFactory.buildBookingStartMessage(room), botKeyboardFactory.mainMenu());
    }

    public void handleBookingCheckIn(Long chatId, String text) {
        LocalDate checkInDate = parseDate(text, chatId);
        if (checkInDate == null) {
            return;
        }
        if (checkInDate.isBefore(today())) {
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

    public void handleBookingCheckOut(Long chatId, String text) {
        LocalDate checkOutDate = parseDate(text, chatId);
        if (checkOutDate == null) {
            return;
        }
        if (checkOutDate.isBefore(today())) {
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
        botMessageService.sendText(chatId, botTextFactory.buildAskBookingAdultsMessage(
                bookingDraft.checkInDate(),
                checkOutDate,
                botFlowProperties.booking().maxAdults(),
                botFlowProperties.booking().maxGuests()
        ), botKeyboardFactory.mainMenu());
    }

    public void handleBookingAdults(Long chatId, String text) {
        Integer adults = parsePositiveInteger(text, botTextFactory.buildAdultsMustBeIntegerMessage(), chatId, botTextFactory.buildPositiveAdultsMessage());
        if (adults == null) {
            return;
        }
        if (adults > botFlowProperties.booking().maxAdults() || adults > botFlowProperties.booking().maxGuests()) {
            botMessageService.sendText(chatId, maxGuestsMessage(), botKeyboardFactory.mainMenu());
            return;
        }

        BookingDraft currentDraft = chatStateService.get(chatId).bookingDraft();
        RoomCategoryResponseDto room = catalogClient.getRoomById(currentDraft.roomCategoryId());
        if (room.maxGuests() != null && adults > room.maxGuests()) {
            botMessageService.sendText(chatId, botTextFactory.buildGuestOverflowMessage(room), botKeyboardFactory.mainMenu());
            return;
        }

        BookingDraft bookingDraft = currentDraft.toBuilder()
                .adultCount(adults)
                .build();
        chatStateService.updateBookingDraft(chatId, bookingDraft);
        chatStateService.setType(chatId, ChatStateType.WAITING_BOOKING_CHILDREN);
        botMessageService.sendText(chatId, botTextFactory.buildAskBookingChildrenMessage(
                adults,
                botFlowProperties.booking().maxChildren(),
                botFlowProperties.booking().maxGuests()
        ), botKeyboardFactory.mainMenu());
    }

    public void handleBookingChildren(Long chatId, String text) {
        Integer children = parseNonNegativeInteger(text, botTextFactory.buildChildrenMustBeIntegerMessage(), chatId, botTextFactory.buildChildrenCountMessage());
        if (children == null) {
            return;
        }
        if (children > botFlowProperties.booking().maxChildren()) {
            botMessageService.sendText(chatId, maxGuestsMessage(), botKeyboardFactory.mainMenu());
            return;
        }

        BookingDraft currentDraft = chatStateService.get(chatId).bookingDraft();
        RoomCategoryResponseDto room = catalogClient.getRoomById(currentDraft.roomCategoryId());
        int totalGuests = safeInt(currentDraft.adultCount()) + children;
        if (totalGuests > botFlowProperties.booking().maxGuests()) {
            botMessageService.sendText(chatId, maxGuestsMessage(), botKeyboardFactory.mainMenu());
            return;
        }
        if (room.maxGuests() != null && totalGuests > room.maxGuests()) {
            botMessageService.sendText(chatId, botTextFactory.buildGuestOverflowMessage(room), botKeyboardFactory.mainMenu());
            return;
        }

        BookingDraft bookingDraft = currentDraft.toBuilder()
                .childrenCount(children)
                .build();
        chatStateService.updateBookingDraft(chatId, bookingDraft);
        showAvailableTariffs(chatId, bookingDraft);
    }

    public void handleTariffSelection(Long chatId, String data) {
        String tariffCode = data.substring("booking:tariff:".length());
        BookingDraft bookingDraft = chatStateService.get(chatId).bookingDraft().toBuilder()
                .tariffCode(tariffCode)
                .build();
        chatStateService.updateBookingDraft(chatId, bookingDraft);
        chatStateService.setType(chatId, ChatStateType.WAITING_BOOKING_PROMO);
        botMessageService.sendText(
                chatId,
                botTextFactory.buildAskBookingPromoMessage(bookingDraft.adultCount(), bookingDraft.childrenCount()),
                botKeyboardFactory.mainMenu()
        );
    }

    public void handleBookingPromo(Long chatId, String text) {
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
        if (totalGuests > botFlowProperties.booking().maxGuests()) {
            botMessageService.sendText(chatId, maxGuestsMessage(), botKeyboardFactory.mainMenu());
            return;
        }
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
                bookingDraft.tariffCode(),
                bookingDraft.promoCode()
        ));

        BotBookingResponseDto actualBooking = awaitBookingResolution(jwt, createdBooking.id());
        BotPaymentResponseDto payment = null;
        if (actualBooking != null && ("HOLD".equalsIgnoreCase(actualBooking.status()) || "CONFIRMED".equalsIgnoreCase(actualBooking.status()))) {
            payment = awaitPaymentResolution(jwt, actualBooking.id());
        }

        rememberAvailabilityDraft(chatId, bookingDraft);
        chatStateService.reset(chatId);
        conversationStore.removeAvailableRooms(chatId);

        if (actualBooking == null) {
            botMessageService.sendText(chatId, botTextFactory.buildUnexpectedErrorMessage(), botKeyboardFactory.mainMenu());
            return;
        }

        if (List.of("FAILED", "EXPIRED", "CANCELLED").contains(actualBooking.status() == null ? "" : actualBooking.status().toUpperCase(Locale.ROOT))) {
            botMessageService.sendText(chatId, botTextFactory.buildBookingFailedMessage(actualBooking), botKeyboardFactory.mainMenu());
            notificationFlowHandler.pushUnreadNotifications(chatId, true);
            return;
        }

        if ("CREATED".equalsIgnoreCase(actualBooking.status())) {
            botMessageService.sendText(chatId, botTextFactory.buildBookingProcessingMessage(actualBooking), botKeyboardFactory.mainMenu());
            return;
        }

        String message = botTextFactory.buildBookingCreatedMessage(actualBooking, room, payment, botProperties.adminContact());
        if (bookingDraft.promoCode() != null
                && !bookingDraft.promoCode().isBlank()
                && (actualBooking.appliedPromoCode() == null || actualBooking.appliedPromoCode().isBlank())) {
            message = botTextFactory.buildPromoCodeIgnoredPrefixMessage(message);
        }

        botMessageService.sendText(chatId, message, botKeyboardFactory.bookingDetails(actualBooking, payment));
        notificationFlowHandler.pushUnreadNotifications(chatId, false);
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
        } catch (ResourceAccessException exception) {
            log.warn("Payment service is unavailable while loading payment. bookingId={}", bookingId, exception);
            return null;
        }
    }

    private void showAvailableTariffs(Long chatId, BookingDraft bookingDraft) {
        int totalGuests = safeInt(bookingDraft.adultCount()) + safeInt(bookingDraft.childrenCount());
        List<BotTariffResponseDto> tariffs = bookingClient.getAvailableTariffs(
                botAuthService.getJwt(chatId),
                new BotBookingRequestDto(
                        bookingDraft.roomCategoryId(),
                        bookingDraft.checkInDate(),
                        bookingDraft.checkOutDate(),
                        totalGuests,
                        bookingDraft.adultCount(),
                        bookingDraft.childrenCount(),
                        null,
                        null
                )
        );
        if (tariffs.isEmpty()) {
            botMessageService.sendText(chatId, botTextFactory.buildUnexpectedErrorMessage(), botKeyboardFactory.mainMenu());
            return;
        }
        botMessageService.sendText(chatId, botTextFactory.buildTariffSelectionMessage(tariffs), botKeyboardFactory.tariffSelection(tariffs));
    }

    private boolean isResolvedBookingStatus(String status) {
        if (status == null) {
            return false;
        }
        return List.of("HOLD", "CONFIRMED", "FAILED", "EXPIRED", "CANCELLED").contains(status.toUpperCase(Locale.ROOT));
    }

    private void rememberAvailabilityDraft(Long chatId, BookingDraft draft) {
        if (draft != null && isValidAvailabilityPeriod(draft.checkInDate(), draft.checkOutDate())) {
            conversationStore.putLastAvailabilityDraft(chatId, AvailableRoomSearchDraft.builder()
                    .checkInDate(draft.checkInDate())
                    .checkOutDate(draft.checkOutDate())
                    .build());
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

    private String valueOrDash(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }

    private Integer availableUnits(RoomCategoryResponseDto room) {
        return room.availableUnits() == null ? room.totalUnits() : room.availableUnits();
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

    private String maxGuestsMessage() {
        return botTextFactory.buildMaxGuestsMessage(
                botFlowProperties.booking().maxGuests(),
                botFlowProperties.booking().maxAdults(),
                botFlowProperties.booking().maxChildren()
        );
    }
}
