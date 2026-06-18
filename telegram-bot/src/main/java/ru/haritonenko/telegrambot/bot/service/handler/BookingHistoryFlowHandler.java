package ru.haritonenko.telegrambot.bot.service.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.haritonenko.commonlibs.dto.category.RoomCategoryResponseDto;
import ru.haritonenko.telegrambot.bot.util.BotKeyboardFactory;
import ru.haritonenko.telegrambot.bot.util.BotTextFactory;
import ru.haritonenko.telegrambot.client.BookingClient;
import ru.haritonenko.telegrambot.client.CatalogClient;
import ru.haritonenko.telegrambot.client.PaymentClient;
import ru.haritonenko.telegrambot.config.BotFlowProperties;
import ru.haritonenko.telegrambot.dto.common.BotPageResponse;
import ru.haritonenko.telegrambot.dto.booking.BotBookingListItem;
import ru.haritonenko.telegrambot.dto.booking.BotBookingResponseDto;
import ru.haritonenko.telegrambot.dto.payment.BotPaymentResponseDto;
import ru.haritonenko.telegrambot.service.BotMessageService;
import ru.haritonenko.telegrambot.service.auth.BotAuthService;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.IntStream;

@Component
@RequiredArgsConstructor
public class BookingHistoryFlowHandler {

    private final BookingClient bookingClient;
    private final CatalogClient catalogClient;
    private final PaymentClient paymentClient;
    private final BotAuthService botAuthService;
    private final BotMessageService botMessageService;
    private final BotKeyboardFactory botKeyboardFactory;
    private final BotTextFactory botTextFactory;
    private final BotFlowProperties botFlowProperties;

    public void sendBookings(Long chatId, Integer messageId, boolean photoMessage) {
        sendBookings(chatId, messageId, photoMessage, 0);
    }

    public void sendBookings(Long chatId, Integer messageId, boolean photoMessage, int pageNumber) {
        String jwt = botAuthService.getJwt(chatId);
        var page = bookingClient.getBookingsPage(jwt, pageNumber, botFlowProperties.pagination().bookingsPageSize());
        List<BotBookingResponseDto> bookings = page == null || page.content() == null ? List.of() : page.content().stream()
                .filter(this::isVisibleBookingForUser)
                .sorted(Comparator.comparing(BotBookingResponseDto::createdAt, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .toList();

        if (bookings.isEmpty()) {
            if (messageId != null && !photoMessage) {
                botMessageService.editText(chatId, messageId, botTextFactory.buildMyBookingsEmptyMessage(), botKeyboardFactory.bookingsList(List.of(), 0, 1, "booking:page:", true, false));
                return;
            }
            botMessageService.sendText(chatId, botTextFactory.buildMyBookingsEmptyMessage(), botKeyboardFactory.bookingsList(List.of(), 0, 1, "booking:page:", true, false));
            return;
        }

        List<BotBookingListItem> bookingItems = buildBookingListItems(bookings);
        int totalPages = page == null ? 0 : page.totalPages();

        if (messageId != null && !photoMessage) {
            botMessageService.editText(chatId, messageId, botTextFactory.buildMyBookingsMessage(bookings), botKeyboardFactory.bookingsList(bookingItems, pageNumber, totalPages, "booking:page:", true, false));
            return;
        }
        botMessageService.sendText(chatId, botTextFactory.buildMyBookingsMessage(bookings), botKeyboardFactory.bookingsList(bookingItems, pageNumber, totalPages, "booking:page:", true, false));
    }

    public void sendInactiveBookings(Long chatId, Integer messageId, boolean photoMessage, int pageNumber) {
        String jwt = botAuthService.getJwt(chatId);
        var page = bookingClient.getInactiveBookingsPage(jwt, pageNumber, botFlowProperties.pagination().bookingsPageSize());
        List<BotBookingResponseDto> bookings = page == null || page.content() == null ? List.of() : page.content();

        if (bookings.isEmpty()) {
            String text = botTextFactory.buildInactiveBookingsEmptyMessage();
            if (messageId != null && !photoMessage) {
                botMessageService.editText(chatId, messageId, text, botKeyboardFactory.bookingsList(List.of(), 0, 1, "booking:inactive:page:", false, true));
                return;
            }
            botMessageService.sendText(chatId, text, botKeyboardFactory.bookingsList(List.of(), 0, 1, "booking:inactive:page:", false, true));
            return;
        }

        List<BotBookingListItem> bookingItems = buildBookingListItems(bookings);
        String text = botTextFactory.buildInactiveBookingsTitleMessage();
        int totalPages = page == null ? 0 : page.totalPages();
        if (messageId != null && !photoMessage) {
            botMessageService.editText(chatId, messageId, text, botKeyboardFactory.bookingsList(bookingItems, pageNumber, totalPages, "booking:inactive:page:", false, true));
            return;
        }
        botMessageService.sendText(chatId, text, botKeyboardFactory.bookingsList(bookingItems, pageNumber, totalPages, "booking:inactive:page:", false, true));
    }

    public void sendEarlyBookings(Long chatId, Integer messageId, boolean photoMessage, int pageNumber) {
        String jwt = botAuthService.getJwt(chatId);
        var page = bookingClient.getEarlyBookingsPage(jwt, pageNumber, botFlowProperties.pagination().bookingsPageSize());
        sendBookingSection(
                chatId,
                messageId,
                photoMessage,
                pageNumber,
                page,
                botTextFactory.buildEarlyBookingsEmptyMessage(),
                botTextFactory.buildEarlyBookingsTitleMessage(),
                "booking:early:page:",
                true
        );
    }

    public void sendHistoryBookings(Long chatId, Integer messageId, boolean photoMessage, int pageNumber) {
        String jwt = botAuthService.getJwt(chatId);
        var page = bookingClient.getHistoryBookingsPage(jwt, pageNumber, botFlowProperties.pagination().bookingsPageSize());
        sendBookingSection(
                chatId,
                messageId,
                photoMessage,
                pageNumber,
                page,
                botTextFactory.buildBookingHistoryEmptyMessage(),
                botTextFactory.buildBookingHistoryTitleMessage(),
                "booking:history:page:",
                true
        );
    }

    public void showBookingDetails(Long chatId, UUID bookingId, Integer messageId, boolean photoMessage) {
        String jwt = botAuthService.getJwt(chatId);
        BotBookingResponseDto booking = bookingClient.getBooking(jwt, bookingId);
        RoomCategoryResponseDto room = getRoomByIdSafely(booking.roomCategoryId());
        BotPaymentResponseDto payment = getPaymentSafely(jwt, bookingId);
        String text = botTextFactory.buildBookingDetails(booking, room, payment);
        if (photoMessage) {
            botMessageService.deleteMessage(chatId, messageId);
            botMessageService.sendText(chatId, text, botKeyboardFactory.bookingDetails(booking, payment));
            return;
        }

        botMessageService.editText(chatId, messageId, text, botKeyboardFactory.bookingDetails(booking, payment));
    }

    private void sendBookingSection(
            Long chatId,
            Integer messageId,
            boolean photoMessage,
            int pageNumber,
            BotPageResponse<BotBookingResponseDto> page,
            String emptyText,
            String title,
            String pagePrefix,
            boolean includeBackToActive
    ) {
        List<BotBookingResponseDto> bookings = page == null || page.content() == null ? List.of() : page.content();
        if (bookings.isEmpty()) {
            if (messageId != null && !photoMessage) {
                botMessageService.editText(chatId, messageId, emptyText, botKeyboardFactory.bookingsList(List.of(), 0, 1, pagePrefix, false, includeBackToActive));
                return;
            }
            botMessageService.sendText(chatId, emptyText, botKeyboardFactory.bookingsList(List.of(), 0, 1, pagePrefix, false, includeBackToActive));
            return;
        }
        List<BotBookingListItem> bookingItems = buildBookingListItems(bookings);
        int totalPages = page == null ? 0 : page.totalPages();
        if (messageId != null && !photoMessage) {
            botMessageService.editText(chatId, messageId, title, botKeyboardFactory.bookingsList(bookingItems, pageNumber, totalPages, pagePrefix, false, includeBackToActive));
            return;
        }
        botMessageService.sendText(chatId, title, botKeyboardFactory.bookingsList(bookingItems, pageNumber, totalPages, pagePrefix, false, includeBackToActive));
    }

    private List<BotBookingListItem> buildBookingListItems(List<BotBookingResponseDto> bookings) {
        return IntStream.range(0, bookings.size())
                .mapToObj(index -> {
                    BotBookingResponseDto booking = bookings.get(index);
                    return new BotBookingListItem(
                            booking.id(),
                            botTextFactory.buildBookingListLabel(index + 1, resolveRoomTitle(booking.roomCategoryId()), booking)
                    );
                })
                .toList();
    }

    private String resolveRoomTitle(Long roomCategoryId) {
        if (roomCategoryId == null) {
            return botTextFactory.buildRoomDefaultTitle();
        }
        try {
            RoomCategoryResponseDto room = catalogClient.getRoomById(roomCategoryId);
            return room == null || room.name() == null ? botTextFactory.buildRoomDefaultTitle() : switch (room.name()) {
                case STANDARD -> "Standard";
                case STANDARD_DOUBLE -> "Standard Double";
                case STANDARD_PLUS -> "Standard Plus";
                case STUDIO -> "Studio";
                case BUSINESS_STUDIO -> "Business Studio";
                case ECONOMY -> "Economy";
            };
        } catch (Exception exception) {
            return botTextFactory.buildRoomDefaultTitle();
        }
    }

    private BotPaymentResponseDto getPaymentSafely(String jwt, UUID bookingId) {
        try {
            return paymentClient.getPaymentByBookingId(jwt, bookingId);
        } catch (Exception exception) {
            return null;
        }
    }

    private RoomCategoryResponseDto getRoomByIdSafely(Long roomCategoryId) {
        if (roomCategoryId == null) {
            return null;
        }
        try {
            return catalogClient.getRoomById(roomCategoryId);
        } catch (Exception exception) {
            return null;
        }
    }

    private boolean isVisibleBookingForUser(BotBookingResponseDto booking) {
        if (booking == null) {
            return false;
        }
        return !isInactiveBookingStatus(booking.status());
    }

    private boolean isInactiveBookingStatus(String status) {
        if (status == null) {
            return false;
        }
        String normalized = status.toUpperCase(Locale.ROOT);
        return normalized.equals("CANCELLED")
                || normalized.equals("EXPIRED")
                || normalized.equals("FAILED");
    }
}
