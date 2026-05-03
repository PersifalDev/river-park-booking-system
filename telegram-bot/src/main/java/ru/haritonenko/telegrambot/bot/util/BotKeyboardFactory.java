package ru.haritonenko.telegrambot.bot.util;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardRow;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import ru.haritonenko.commonlibs.dto.service.ServiceItemResponseDto;
import ru.haritonenko.telegrambot.dto.booking.BotBookingListItem;
import ru.haritonenko.telegrambot.dto.booking.BotBookingResponseDto;
import ru.haritonenko.telegrambot.dto.payment.BotPaymentResponseDto;

import java.util.ArrayList;
import java.util.List;

@Component
public class BotKeyboardFactory {

    public ReplyKeyboardMarkup mainMenu() {
        KeyboardRow row1 = new KeyboardRow();
        row1.add(new KeyboardButton("Подобрать номер"));
        row1.add(new KeyboardButton("Все номера"));

        KeyboardRow row2 = new KeyboardRow();
        row2.add(new KeyboardButton("Мои брони"));
        row2.add(new KeyboardButton("Услуги"));

        KeyboardRow row3 = new KeyboardRow();
        row3.add(new KeyboardButton("Правила проживания"));
        row3.add(new KeyboardButton("Контакты"));

        return ReplyKeyboardMarkup.builder()
                .resizeKeyboard(true)
                .isPersistent(true)
                .keyboard(List.of(row1, row2, row3))
                .build();
    }

    public InlineKeyboardMarkup inlineMainMenu() {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        row(button("Подобрать номер", "menu:pick-room"), button("Все номера", "menu:all-rooms")),
                        row(button("Мои брони", "menu:my-bookings"), button("Услуги", "menu:services")),
                        row(button("Правила проживания", "menu:rules"), button("Контакты", "menu:contacts"))
                ))
                .build();
    }

    public InlineKeyboardMarkup roomTypeSelection() {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        row(button("Standard", "filter:room-type:STANDARD"), button("Standard Double", "filter:room-type:STANDARD_DOUBLE")),
                        row(button("Standard Plus", "filter:room-type:STANDARD_PLUS"), button("Studio", "filter:room-type:STUDIO")),
                        row(button("Business Studio", "filter:room-type:BUSINESS_STUDIO"), button("Economy", "filter:room-type:ECONOMY")),
                        row(button("Без категории", "filter:room-type:skip")),
                        row(button("Меню", "menu:main"))
                ))
                .build();
    }

    public InlineKeyboardMarkup roomCard(Long roomId, int pageNumber, int totalPages) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(row(
                button("Подробнее", "room:view:" + roomId + ":" + pageNumber),
                button("Фото", "room:photos:" + roomId + ":" + pageNumber)
        ));
        rows.add(row(button("Забронировать", "booking:start:" + roomId)));
        rows.add(paginationRow(pageNumber, totalPages, "rooms:page:"));
        rows.add(row(button("Меню", "menu:main")));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public InlineKeyboardMarkup roomDetails(Long roomId, int pageNumber) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(row(
                button("Забронировать", "booking:start:" + roomId),
                button("Фото", "room:photos:" + roomId + ":" + pageNumber)
        ));
        rows.add(row(button("К списку номеров", "rooms:page:" + pageNumber)));
        rows.add(row(button("Меню", "menu:main")));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public InlineKeyboardMarkup photoGallery(Long roomId, int photoIndex, int totalPhotos, int roomPageNumber) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(photoPaginationRow(roomId, photoIndex, totalPhotos, roomPageNumber));
        rows.add(row(
                button("Подробнее", "room:view:" + roomId + ":" + roomPageNumber),
                button("Забронировать", "booking:start:" + roomId)
        ));
        rows.add(row(button("Назад к подборке", "rooms:page:" + roomPageNumber)));
        rows.add(row(button("Все номера", "menu:all-rooms"), button("Меню", "menu:main")));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public InlineKeyboardMarkup servicesPage(List<ServiceItemResponseDto> services, int pageNumber, boolean hasNextPage) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (ServiceItemResponseDto service : services) {
            rows.add(row(button(service.title(), "service:view:" + service.id() + ":" + pageNumber)));
        }
        rows.add(simplePaginationRow(pageNumber, hasNextPage, "services:page:"));
        rows.add(row(button("Меню", "menu:main")));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public InlineKeyboardMarkup serviceDetails(Long serviceId, int pageNumber) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        rows.add(row(button("К списку услуг", "services:page:" + pageNumber)));
        rows.add(row(button("Меню", "menu:main")));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public InlineKeyboardMarkup rulesKeyboard() {
        return InlineKeyboardMarkup.builder()
                .keyboard(List.of(
                        row(button("Отправить PDF", "rules:file")),
                        row(button("Меню", "menu:main"))
                ))
                .build();
    }

    public InlineKeyboardMarkup bookingsList(List<BotBookingListItem> bookings) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        for (BotBookingListItem booking : bookings) {
            rows.add(row(button(booking.label(), "booking:view:" + booking.bookingId())));
        }
        rows.add(row(button("Меню", "menu:main")));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    public InlineKeyboardMarkup bookingDetails(BotBookingResponseDto booking, BotPaymentResponseDto payment) {
        List<InlineKeyboardRow> rows = new ArrayList<>();
        if (canConfirmPayment(booking, payment)) {
            rows.add(row(button("Подтвердить бронь", "payment:confirm:" + booking.id())));
        }
        if (canCancelThroughPayment(booking, payment)) {
            rows.add(row(button("Отменить бронь", "payment:cancel:" + booking.id())));
        } else if (canCancelBooking(booking)) {
            rows.add(row(button("Отменить бронь", "booking:cancel:" + booking.id())));
        }
        rows.add(row(button("К списку броней", "menu:my-bookings"), button("Меню", "menu:main")));
        return InlineKeyboardMarkup.builder().keyboard(rows).build();
    }

    private boolean canConfirmPayment(BotBookingResponseDto booking, BotPaymentResponseDto payment) {
        return booking != null
                && "HOLD".equalsIgnoreCase(booking.status())
                && payment != null
                && "PENDING".equalsIgnoreCase(payment.status());
    }

    private boolean canCancelThroughPayment(BotBookingResponseDto booking, BotPaymentResponseDto payment) {
        return booking != null
                && payment != null
                && "PENDING".equalsIgnoreCase(payment.status())
                && ("HOLD".equalsIgnoreCase(booking.status()) || "CREATED".equalsIgnoreCase(booking.status()));
    }

    private boolean canCancelBooking(BotBookingResponseDto booking) {
        return booking != null
                && booking.status() != null
                && List.of("HOLD", "CREATED", "CONFIRMED").contains(booking.status().toUpperCase());
    }

    private InlineKeyboardRow paginationRow(int pageNumber, int totalPages, String prefix) {
        InlineKeyboardRow row = new InlineKeyboardRow();
        if (pageNumber > 0) {
            row.add(button("⬅️", prefix + (pageNumber - 1)));
        }
        if (pageNumber + 1 < totalPages) {
            row.add(button("➡️", prefix + (pageNumber + 1)));
        }
        return row;
    }

    private InlineKeyboardRow photoPaginationRow(Long roomId, int photoIndex, int totalPhotos, int roomPageNumber) {
        InlineKeyboardRow row = new InlineKeyboardRow();
        if (photoIndex > 0) {
            row.add(button("⬅️", "room:photo:index:" + roomId + ":" + (photoIndex - 1) + ":" + roomPageNumber));
        }
        if (photoIndex + 1 < totalPhotos) {
            row.add(button("➡️", "room:photo:index:" + roomId + ":" + (photoIndex + 1) + ":" + roomPageNumber));
        }
        return row;
    }

    private InlineKeyboardRow simplePaginationRow(int pageNumber, boolean hasNextPage, String prefix) {
        InlineKeyboardRow row = new InlineKeyboardRow();
        if (pageNumber > 0) {
            row.add(button("⬅️", prefix + (pageNumber - 1)));
        }
        if (hasNextPage) {
            row.add(button("➡️", prefix + (pageNumber + 1)));
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
}
