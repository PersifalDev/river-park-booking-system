package ru.haritonenko.telegrambot.bot.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ru.haritonenko.commonlibs.dto.service.ServiceItemResponseDto;
import ru.haritonenko.telegrambot.bot.command.BotMenuCommand;
import ru.haritonenko.telegrambot.config.BotMessagesProperties;
import ru.haritonenko.telegrambot.bot.state.AvailableRoomSearchDraft;
import ru.haritonenko.telegrambot.dto.booking.BotBookingListItem;
import ru.haritonenko.telegrambot.dto.booking.BotBookingResponseDto;
import ru.haritonenko.telegrambot.dto.booking.BotTariffResponseDto;
import ru.haritonenko.telegrambot.dto.payment.BotPaymentResponseDto;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class BotKeyboardFactory {

    private final BotMessagesProperties messages;

    public Optional<BotMenuCommand> resolveMenuCommand(String text) {
        return BotMenuCommand.fromText(text, messages.keyboard());
    }

    public ReplyKeyboardMarkup mainMenu() {
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton(label(BotMenuCommand.PICK_ROOM)));
        row1.add(new KeyboardButton(label(BotMenuCommand.ALL_ROOMS)));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton(label(BotMenuCommand.MY_BOOKINGS)));
        row2.add(new KeyboardButton(label(BotMenuCommand.SERVICES)));

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton(label(BotMenuCommand.RULES)));
        row3.add(new KeyboardButton(label(BotMenuCommand.CONTACTS)));

        return ReplyKeyboardMarkup.builder()
                .resizeKeyboard(true)
                .isPersistent(true)
                .keyboard(List.of(row1, row2, row3))
                .build();
    }

    public InlineKeyboardMarkup inlineMainMenu() {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        row(button(label(BotMenuCommand.PICK_ROOM), "menu:pick-room"), button(label(BotMenuCommand.ALL_ROOMS), "menu:all-rooms")),
                        row(button(label(BotMenuCommand.MY_BOOKINGS), "menu:my-bookings"), button(label(BotMenuCommand.SERVICES), "menu:services")),
                        row(button(label(BotMenuCommand.RULES), "menu:rules"), button(label(BotMenuCommand.CONTACTS), "menu:contacts"))
                ))
                .build();
    }

    public InlineKeyboardMarkup roomTypeSelection() {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        row(button("Standard", "filter:room-type:STANDARD"), button("Standard Double", "filter:room-type:STANDARD_DOUBLE")),
                        row(button("Standard Plus", "filter:room-type:STANDARD_PLUS"), button("Studio", "filter:room-type:STUDIO")),
                        row(button("Business Studio", "filter:room-type:BUSINESS_STUDIO"), button("Economy", "filter:room-type:ECONOMY")),
                        row(button(messages.keyboard().noCategory(), "filter:room-type:skip")),
                        row(button(messages.keyboard().filterSearch(), "filter:search"), button(messages.keyboard().menu(), "menu:main"))
                ))
                .build();
    }

    public InlineKeyboardMarkup roomFilterMenu(AvailableRoomSearchDraft draft) {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        row(button(messages.keyboard().filterGuests(), "filter:guests"), button(messages.keyboard().filterDates(), "filter:dates")),
                        row(button(messages.keyboard().filterRoomType(), "filter:room-type:open"), button(messages.keyboard().filterPrice(), "filter:price")),
                        row(button(messages.keyboard().filterArea(), "filter:area")),
                        row(button(messages.keyboard().filterSearch(), "filter:search"), button(messages.keyboard().filterReset(), "filter:reset")),
                        row(button(messages.keyboard().menu(), "menu:main"))
                ))
                .build();
    }

    public InlineKeyboardMarkup roomCard(Long roomId, int pageNumber, int totalPages) {
        return roomCard(roomId, pageNumber, totalPages, "rooms:page:");
    }

    public InlineKeyboardMarkup roomCard(Long roomId, int pageNumber, int totalPages, String pagePrefix) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        String sourceSuffix = "rooms:filter:page:".equals(pagePrefix) ? ":filter" : "";
        rows.add(row(
                button(messages.keyboard().details(), "room:view:" + roomId + ":" + pageNumber + sourceSuffix),
                button(messages.keyboard().photo(), "room:photos:" + roomId + ":" + pageNumber + sourceSuffix)
        ));
        rows.add(row(button(messages.keyboard().book(), "booking:start:" + roomId)));
        rows.add(paginationRow(pageNumber, totalPages, pagePrefix));
        rows.add(row(button(messages.keyboard().menu(), "menu:main")));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public InlineKeyboardMarkup roomDetails(Long roomId, int pageNumber) {
        return roomDetails(roomId, pageNumber, false);
    }

    public InlineKeyboardMarkup roomDetails(Long roomId, int pageNumber, boolean filtered) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        String sourceSuffix = filtered ? ":filter" : "";
        String pagePrefix = filtered ? "rooms:filter:page:" : "rooms:page:";
        rows.add(row(
                button(messages.keyboard().book(), "booking:start:" + roomId),
                button(messages.keyboard().photo(), "room:photos:" + roomId + ":" + pageNumber + sourceSuffix)
        ));
        rows.add(row(button(messages.keyboard().backToRooms(), pagePrefix + pageNumber)));
        rows.add(row(button(messages.keyboard().menu(), "menu:main")));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public InlineKeyboardMarkup photoGallery(Long roomId, int photoIndex, int totalPhotos, int roomPageNumber) {
        return photoGallery(roomId, photoIndex, totalPhotos, roomPageNumber, false);
    }

    public InlineKeyboardMarkup photoGallery(Long roomId, int photoIndex, int totalPhotos, int roomPageNumber, boolean filtered) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(photoPaginationRow(roomId, photoIndex, totalPhotos, roomPageNumber, filtered));
        String pagePrefix = filtered ? "rooms:filter:page:" : "rooms:page:";
        String sourceSuffix = filtered ? ":filter" : "";
        rows.add(row(
                button(messages.keyboard().details(), "room:view:" + roomId + ":" + roomPageNumber + sourceSuffix),
                button(messages.keyboard().book(), "booking:start:" + roomId)
        ));
        rows.add(row(button(messages.keyboard().backToSelection(), pagePrefix + roomPageNumber)));
        rows.add(row(button(label(BotMenuCommand.ALL_ROOMS), "menu:all-rooms"), button(messages.keyboard().menu(), "menu:main")));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public InlineKeyboardMarkup servicesPage(List<ServiceItemResponseDto> services, int pageNumber, boolean hasNextPage) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (ServiceItemResponseDto service : services) {
            rows.add(row(button(service.title(), "service:view:" + service.id() + ":" + pageNumber)));
        }
        rows.add(simplePaginationRow(pageNumber, hasNextPage, "services:page:"));
        rows.add(row(button(messages.keyboard().menu(), "menu:main")));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public InlineKeyboardMarkup serviceDetails(Long serviceId, int pageNumber) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(row(button(messages.keyboard().backToServices(), "services:page:" + pageNumber)));
        rows.add(row(button(messages.keyboard().menu(), "menu:main")));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public InlineKeyboardMarkup rulesKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        row(button(messages.keyboard().sendPdf(), "rules:file")),
                        row(button(messages.keyboard().menu(), "menu:main"))
                ))
                .build();
    }

    public InlineKeyboardMarkup bookingsList(List<BotBookingListItem> bookings) {
        return bookingsList(bookings, 0, 1, "booking:page:", true);
    }

    public InlineKeyboardMarkup bookingsList(
            List<BotBookingListItem> bookings,
            int pageNumber,
            int totalPages,
            String pagePrefix,
            boolean includeInactiveLink
    ) {
        return bookingsList(bookings, pageNumber, totalPages, pagePrefix, includeInactiveLink, false);
    }

    public InlineKeyboardMarkup bookingsList(
            List<BotBookingListItem> bookings,
            int pageNumber,
            int totalPages,
            String pagePrefix,
            boolean includeSections,
            boolean includeBackToActive
    ) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (BotBookingListItem booking : bookings) {
            rows.add(row(button(booking.label(), "booking:view:" + booking.bookingId())));
        }
        rows.add(paginationRow(pageNumber, totalPages, pagePrefix));
        if (includeSections) {
            rows.add(row(button(messages.keyboard().inactiveBookings(), "booking:inactive"), button(messages.keyboard().earlyBookings(), "booking:early")));
            rows.add(row(button(messages.keyboard().bookingHistory(), "booking:history")));
        }
        if (includeBackToActive) {
            rows.add(row(button(messages.keyboard().backToActiveBookings(), "menu:my-bookings")));
        }
        rows.add(row(button(messages.keyboard().menu(), "menu:main")));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public InlineKeyboardMarkup bookingDetails(BotBookingResponseDto booking, BotPaymentResponseDto payment) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        if (canConfirmPayment(booking, payment)) {
            rows.add(row(button(messages.keyboard().confirmBooking(), "payment:confirm:" + booking.id())));
        }
        if (canCancelBooking(booking)) {
            rows.add(row(button(messages.keyboard().cancelBooking(), "booking:cancel:" + booking.id())));
        }
        rows.add(row(button(messages.keyboard().backToBookings(), "menu:my-bookings"), button(messages.keyboard().menu(), "menu:main")));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public InlineKeyboardMarkup tariffSelection(List<BotTariffResponseDto> tariffs) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (BotTariffResponseDto tariff : tariffs) {
            rows.add(row(button(tariff.title(), "booking:tariff:" + tariff.code())));
        }
        rows.add(row(button(messages.keyboard().menu(), "menu:main")));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private boolean canConfirmPayment(BotBookingResponseDto booking, BotPaymentResponseDto payment) {
        return booking != null
                && "HOLD".equalsIgnoreCase(booking.status())
                && payment != null
                && "PENDING".equalsIgnoreCase(payment.status());
    }

    private boolean canCancelBooking(BotBookingResponseDto booking) {
        return booking != null
                && booking.status() != null
                && List.of("HOLD", "CREATED", "CONFIRMED").contains(booking.status().toUpperCase());
    }

    private InlineKeyboardRow paginationRow(int pageNumber, int totalPages, String prefix) {
        InlineKeyboardRow row = new InlineKeyboardRow();
        if (pageNumber > 0) {
            row.add(button(messages.keyboard().previous(), prefix + (pageNumber - 1)));
        }
        if (pageNumber + 1 < totalPages) {
            row.add(button(messages.keyboard().next(), prefix + (pageNumber + 1)));
        }
        return row;
    }

    private InlineKeyboardRow photoPaginationRow(Long roomId, int photoIndex, int totalPhotos, int roomPageNumber) {
        return photoPaginationRow(roomId, photoIndex, totalPhotos, roomPageNumber, false);
    }

    private InlineKeyboardRow photoPaginationRow(Long roomId, int photoIndex, int totalPhotos, int roomPageNumber, boolean filtered) {
        InlineKeyboardRow row = new InlineKeyboardRow();
        String sourceSuffix = filtered ? ":filter" : "";
        if (photoIndex > 0) {
            row.add(button(messages.keyboard().previous(), "room:photo:index:" + roomId + ":" + (photoIndex - 1) + ":" + roomPageNumber + sourceSuffix));
        }
        if (photoIndex + 1 < totalPhotos) {
            row.add(button(messages.keyboard().next(), "room:photo:index:" + roomId + ":" + (photoIndex + 1) + ":" + roomPageNumber + sourceSuffix));
        }
        return row;
    }

    private InlineKeyboardRow simplePaginationRow(int pageNumber, boolean hasNextPage, String prefix) {
        InlineKeyboardRow row = new InlineKeyboardRow();
        if (pageNumber > 0) {
            row.add(button(messages.keyboard().previous(), prefix + (pageNumber - 1)));
        }
        if (hasNextPage) {
            row.add(button(messages.keyboard().next(), prefix + (pageNumber + 1)));
        }
        return row;
    }

    private InlineKeyboardRow row(InlineKeyboardButton... buttons) {
        InlineKeyboardRow row = new InlineKeyboardRow();
        row.addAll(List.of(buttons));
        return row;
    }

    private InlineKeyboardButton button(String text, String data) {
        return InlineKeyboardButton.builder()
                .text(text)
                .callbackData(data)
                .build();
    }

    private String label(BotMenuCommand command) {
        return command.label(messages.keyboard());
    }
}
