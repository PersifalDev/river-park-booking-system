package ru.haritonenko.telegrambot.bot.service.handler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;
import ru.haritonenko.commonlibs.dto.category.RoomCategoryResponseDto;
import ru.haritonenko.telegrambot.bot.service.storage.BotConversationStore;
import ru.haritonenko.telegrambot.bot.state.AvailableRoomSearchDraft;
import ru.haritonenko.telegrambot.bot.util.BotKeyboardFactory;
import ru.haritonenko.telegrambot.bot.util.BotTextFactory;
import ru.haritonenko.telegrambot.client.BookingClient;
import ru.haritonenko.telegrambot.client.CatalogClient;
import ru.haritonenko.telegrambot.client.PaymentClient;
import ru.haritonenko.telegrambot.dto.booking.BotBookingResponseDto;
import ru.haritonenko.telegrambot.dto.payment.BotPaymentResponseDto;
import ru.haritonenko.telegrambot.service.BotMessageService;
import ru.haritonenko.telegrambot.service.auth.BotAuthService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class BookingActionFlowHandler {

    private static final ZoneId NOVOSIBIRSK_ZONE = ZoneId.of("Asia/Novosibirsk");

    private final BookingClient bookingClient;
    private final CatalogClient catalogClient;
    private final PaymentClient paymentClient;
    private final BotAuthService botAuthService;
    private final BotMessageService botMessageService;
    private final BotKeyboardFactory botKeyboardFactory;
    private final BotTextFactory botTextFactory;
    private final BotConversationStore conversationStore;
    private final NotificationFlowHandler notificationFlowHandler;

    public void cancelBooking(Long chatId, UUID bookingId, Integer messageId, boolean photoMessage) {
        String jwt = botAuthService.getJwt(chatId);
        if (!photoMessage) {
            botMessageService.editText(chatId, messageId, botTextFactory.buildCancellingBookingMessage(), botKeyboardFactory.inlineMainMenu());
        }
        BotBookingResponseDto cancelledBooking;
        try {
            cancelledBooking = bookingClient.cancelBooking(jwt, bookingId);
        } catch (RestClientResponseException exception) {
            if (!isAlreadyInactiveBookingError(exception)) {
                throw exception;
            }
            cancelledBooking = bookingClient.getBooking(jwt, bookingId);
        }
        conversationStore.removeAvailableRooms(chatId);
        RoomCategoryResponseDto room = getRoomByIdSafely(cancelledBooking.roomCategoryId());
        BotPaymentResponseDto payment = ensurePaymentCancelledAfterBookingCancellation(jwt, bookingId);
        sendActionResult(
                chatId,
                messageId,
                photoMessage,
                botTextFactory.buildBookingDetails(cancelledBooking, room, payment),
                botKeyboardFactory.bookingDetails(cancelledBooking, payment)
        );
        notificationFlowHandler.pushUnreadNotifications(chatId, false);
    }

    public void confirmPayment(Long chatId, UUID bookingId, Integer messageId, boolean photoMessage) {
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
        rememberAvailabilityDraft(chatId, booking);
        conversationStore.removeAvailableRooms(chatId);
        sendActionResult(chatId, messageId, photoMessage, botTextFactory.buildPaymentConfirmedMessage(booking), botKeyboardFactory.bookingDetails(booking, payment));
        notificationFlowHandler.pushUnreadNotifications(chatId, false);
    }

    public void cancelPayment(Long chatId, UUID bookingId, Integer messageId, boolean photoMessage) {
        String jwt = botAuthService.getJwt(chatId);
        boolean paymentCancelled = false;
        try {
            paymentClient.cancelPayment(jwt, bookingId);
            paymentCancelled = true;
        } catch (RestClientResponseException exception) {
            if (!isAlreadyInactivePaymentError(exception)) {
                throw exception;
            }
        } catch (ResourceAccessException exception) {
            log.warn("Payment service is unavailable during cancellation. bookingId={}", bookingId, exception);
        }

        BotBookingResponseDto booking = paymentCancelled
                ? awaitBookingCancellation(jwt, bookingId)
                : cancelBookingDirectly(jwt, bookingId);
        BotPaymentResponseDto payment = getPaymentSafely(jwt, bookingId);
        if (booking == null) {
            sendActionResult(chatId, messageId, photoMessage, botTextFactory.buildUnexpectedErrorMessage(), botKeyboardFactory.inlineMainMenu());
            return;
        }
        RoomCategoryResponseDto room = getRoomByIdSafely(booking.roomCategoryId());
        sendActionResult(
                chatId,
                messageId,
                photoMessage,
                botTextFactory.buildBookingDetails(booking, room, payment),
                botKeyboardFactory.bookingDetails(booking, payment)
        );
        notificationFlowHandler.pushUnreadNotifications(chatId, false);
    }

    private BotBookingResponseDto awaitBookingCancellation(String jwt, UUID bookingId) {
        for (int attempt = 0; attempt < 6; attempt++) {
            BotBookingResponseDto booking = bookingClient.getBooking(jwt, bookingId);
            if (booking != null && isInactiveBookingStatus(booking.status())) {
                return booking;
            }
            sleep(500);
        }
        return bookingClient.getBooking(jwt, bookingId);
    }

    private BotBookingResponseDto awaitBookingConfirmation(String jwt, UUID bookingId) {
        for (int attempt = 0; attempt < 6; attempt++) {
            BotBookingResponseDto booking = bookingClient.getBooking(jwt, bookingId);
            if (booking != null && isConfirmedBookingStatus(booking.status())) {
                return booking;
            }
            sleep(500);
        }
        return bookingClient.getBooking(jwt, bookingId);
    }

    private BotPaymentResponseDto awaitPaymentResolution(String jwt, UUID bookingId) {
        for (int attempt = 0; attempt < 6; attempt++) {
            BotPaymentResponseDto payment = getPaymentSafely(jwt, bookingId);
            if (payment != null && payment.status() != null) {
                String status = payment.status().toUpperCase(Locale.ROOT);
                if (List.of("PENDING_CONFIRMATION", "CONFIRMED", "CANCELLED", "FAILED").contains(status)) {
                    return payment;
                }
            }
            sleep(500);
        }
        return getPaymentSafely(jwt, bookingId);
    }

    private BotPaymentResponseDto ensurePaymentCancelledAfterBookingCancellation(String jwt, UUID bookingId) {
        BotPaymentResponseDto payment = getPaymentSafely(jwt, bookingId);
        if (payment == null || isInactivePaymentStatus(payment.status())) {
            return payment;
        }
        cancelPaymentAfterBookingCancellation(jwt, bookingId);
        return awaitPaymentCancellation(jwt, bookingId);
    }

    private BotPaymentResponseDto awaitPaymentCancellation(String jwt, UUID bookingId) {
        for (int attempt = 0; attempt < 6; attempt++) {
            BotPaymentResponseDto payment = getPaymentSafely(jwt, bookingId);
            if (payment == null || isInactivePaymentStatus(payment.status())) {
                return payment;
            }
            sleep(500);
        }
        return getPaymentSafely(jwt, bookingId);
    }

    private void cancelPaymentAfterBookingCancellation(String jwt, UUID bookingId) {
        try {
            paymentClient.cancelPayment(jwt, bookingId);
        } catch (RestClientResponseException exception) {
            if (!isAlreadyInactivePaymentError(exception)) {
                throw exception;
            }
        } catch (ResourceAccessException exception) {
            log.warn("Payment service is unavailable after booking cancellation. bookingId={}", bookingId, exception);
        }
    }

    private BotBookingResponseDto cancelBookingDirectly(String jwt, UUID bookingId) {
        try {
            return bookingClient.cancelBooking(jwt, bookingId);
        } catch (RestClientResponseException exception) {
            if (!isAlreadyInactiveBookingError(exception)) {
                throw exception;
            }
            return bookingClient.getBooking(jwt, bookingId);
        }
    }

    private BotPaymentResponseDto getPaymentSafely(String jwt, UUID bookingId) {
        try {
            return paymentClient.getPaymentByBookingId(jwt, bookingId);
        } catch (Exception exception) {
            log.debug("Payment was not loaded for bookingId={}", bookingId, exception);
            return null;
        }
    }

    private RoomCategoryResponseDto getRoomByIdSafely(Long roomCategoryId) {
        if (roomCategoryId == null) {
            return null;
        }
        try {
            return catalogClient.getRoomById(roomCategoryId);
        } catch (RestClientResponseException | ResourceAccessException exception) {
            log.warn("Failed to load room category for display. roomCategoryId={}", roomCategoryId, exception);
            return null;
        }
    }

    private void rememberAvailabilityDraft(Long chatId, BotBookingResponseDto booking) {
        if (booking != null && isValidAvailabilityPeriod(booking.checkInDate(), booking.checkOutDate())) {
            conversationStore.putLastAvailabilityDraft(chatId, AvailableRoomSearchDraft.builder()
                    .checkInDate(booking.checkInDate())
                    .checkOutDate(booking.checkOutDate())
                    .build());
        }
    }

    private boolean isValidAvailabilityPeriod(LocalDate checkInDate, LocalDate checkOutDate) {
        return checkInDate != null
                && checkOutDate != null
                && !checkInDate.isBefore(LocalDate.now(NOVOSIBIRSK_ZONE))
                && checkOutDate.isAfter(checkInDate);
    }

    private boolean isInactiveBookingStatus(String status) {
        if (status == null) {
            return false;
        }
        return List.of("FAILED", "EXPIRED", "CANCELLED").contains(status.toUpperCase(Locale.ROOT));
    }

    private boolean isConfirmedBookingStatus(String status) {
        if (status == null) {
            return false;
        }
        return List.of("CONFIRMED", "FAILED", "EXPIRED", "CANCELLED").contains(status.toUpperCase(Locale.ROOT));
    }

    private boolean isInactivePaymentStatus(String status) {
        if (status == null) {
            return false;
        }
        return List.of("CANCELLED", "FAILED").contains(status.toUpperCase(Locale.ROOT));
    }

    private boolean isAlreadyInactiveBookingError(RestClientResponseException exception) {
        return containsAlreadyInactiveBooking(
                exception.getResponseBodyAsString(),
                exception.getMessage()
        );
    }

    private boolean isAlreadyInactivePaymentError(RestClientResponseException exception) {
        return containsAlreadyInactivePayment(
                exception.getResponseBodyAsString(),
                exception.getMessage()
        );
    }

    private boolean containsAlreadyInactiveBooking(String... messages) {
        if (messages == null) {
            return false;
        }
        for (String message : messages) {
            if (message != null && message.toLowerCase(Locale.ROOT).contains("booking already inactive")) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAlreadyInactivePayment(String... messages) {
        if (messages == null) {
            return false;
        }
        for (String message : messages) {
            if (message != null && message.toLowerCase(Locale.ROOT).contains("payment already inactive")) {
                return true;
            }
        }
        return false;
    }

    private void sendActionResult(Long chatId, Integer messageId, boolean photoMessage, String text, org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup keyboard) {
        if (photoMessage) {
            botMessageService.deleteMessage(chatId, messageId);
            botMessageService.sendText(chatId, text, keyboard);
            return;
        }
        botMessageService.editText(chatId, messageId, text, keyboard);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while waiting for booking operation", exception);
        }
    }
}
