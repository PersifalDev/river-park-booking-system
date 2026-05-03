package ru.haritonenko.telegrambot.bot.util;

import org.springframework.stereotype.Component;
import ru.haritonenko.commonlibs.dto.category.RoomCategoryResponseDto;
import ru.haritonenko.commonlibs.dto.category.RoomCategorySearchRequestDto;
import ru.haritonenko.commonlibs.dto.category.type.RoomType;
import ru.haritonenko.commonlibs.dto.rule.RuleDocumentResponseDto;
import ru.haritonenko.commonlibs.dto.service.ServiceItemResponseDto;
import ru.haritonenko.telegrambot.dto.booking.BotBookingResponseDto;
import ru.haritonenko.telegrambot.dto.notification.BotNotificationResponseDto;
import ru.haritonenko.telegrambot.dto.payment.BotPaymentResponseDto;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Component
public class BotTextFactory {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter SHORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    public String buildStartMessage() {
        return "Здравствуйте. Я бот River Park.\n\n"
                + "Помогу подобрать номер, оформить бронь, показать услуги и прислать уведомления по заявке.\n\n"
                + "Команда /site — официальный сайт отеля.";
    }

    public String buildMenuMessage() {
        return "Выберите нужный раздел ниже.";
    }

    public String buildFilterStartMessage() {
        return "Подбор номера.\n\nВведите количество гостей числом.\nНапример: 2";
    }

    public String buildAskFilterCheckInMessage() {
        return "Подбор номера.\n\nВведите дату заезда в формате ДД.ММ.ГГГГ.";
    }

    public String buildAskFilterCheckOutMessage(LocalDate checkInDate) {
        return "Дата заезда: " + formatDate(checkInDate) + "\n\nТеперь введите дату выезда в формате ДД.ММ.ГГГГ.";
    }

    public String buildAskRoomTypeMessage() {
        return "Выберите категорию номера кнопками ниже.\n\nМожно выбрать конкретную категорию или продолжить без неё.";
    }

    public String buildAskPriceFromMessage() {
        return "Введите минимальную цену. Если фильтр не нужен, отправьте -";
    }

    public String buildAskPriceToMessage() {
        return "Введите максимальную цену. Если фильтр не нужен, отправьте -";
    }

    public String buildAskMinAreaMessage() {
        return "Введите минимальную площадь в м². Если фильтр не нужен, отправьте -";
    }

    public String buildFilterSummary(RoomCategorySearchRequestDto filter) {
        return "Параметры подбора:\n\n"
                + "Гости: " + valueOrDash(filter.guests()) + "\n"
                + "Категория: " + (filter.roomType() == null ? "Любая" : roomTypeTitle(filter.roomType())) + "\n"
                + "Цена от: " + valueOrDash(filter.priceFrom()) + "\n"
                + "Цена до: " + valueOrDash(filter.priceTo()) + "\n"
                + "Мин. площадь: " + valueOrDash(filter.minArea());
    }

    public String buildRoomSelectionMessage(List<RoomCategoryResponseDto> rooms) {
        StringBuilder builder = new StringBuilder("Доступные номера River Park:\n\n");
        for (RoomCategoryResponseDto room : rooms) {
            builder.append(room.id())
                    .append(". ")
                    .append(roomTypeTitle(room.name()))
                    .append("\n");
        }
        builder.append("\nВведите номер нужной категории.");
        return builder.toString();
    }

    public String buildRoomCard(RoomCategoryResponseDto room, int pageNumber, int totalPages) {
        return buildRoomCard(room, pageNumber, totalPages, false);
    }

    public String buildRoomCard(RoomCategoryResponseDto room, int pageNumber, int totalPages, boolean filtered) {
        StringBuilder builder = new StringBuilder();
        builder.append(filtered ? "Подходящие номера River Park\n" : "Номера River Park\n")
                .append("Страница ")
                .append(pageNumber + 1)
                .append(" из ")
                .append(Math.max(totalPages, 1))
                .append("\n\n")
                .append(roomTypeTitle(room.name()))
                .append("\n")
                .append("Категория: ").append(room.id()).append("\n")
                .append("Гостей: ").append(valueOrDash(room.maxGuests())).append("\n")
                .append("Цена: ").append(formatPrice(room.basePrice())).append(" за ночь\n")
                .append("Площадь: ").append(formatArea(room.areaSquare())).append("\n")
                .append(filtered ? "Свободно номеров: " : "Количество номеров: ")
                .append(valueOrDash(room.totalUnits()));
        return builder.toString();
    }

    public String buildRoomDetails(RoomCategoryResponseDto room) {
        StringBuilder builder = new StringBuilder();
        builder.append(roomTypeTitle(room.name())).append("\n")
                .append("Категория: ").append(room.id()).append("\n")
                .append("Гостей: ").append(valueOrDash(room.maxGuests())).append("\n")
                .append("Цена за ночь: ").append(formatPrice(room.basePrice())).append("\n")
                .append("Площадь: ").append(formatArea(room.areaSquare())).append("\n")
                .append("Количество номеров: ").append(valueOrDash(room.totalUnits()));
        if (room.description() != null && !room.description().isBlank()) {
            builder.append("\n\n").append(room.description());
        }
        return builder.toString();
    }

    public String buildPhotosLoading(RoomCategoryResponseDto room) {
        return "Загружаю фотографии номера «" + roomTypeTitle(room.name()) + "».";
    }

    public String buildPhotosCaption(RoomCategoryResponseDto room, int currentIndex, int totalPhotos) {
        return "Фотографии номера\n\n"
                + roomTypeTitle(room.name()) + "\n"
                + "Категория: " + room.id() + "\n"
                + "Фото " + (currentIndex + 1) + " из " + Math.max(totalPhotos, 1);
    }

    public String buildServicesMessage(List<ServiceItemResponseDto> services, int pageNumber) {
        StringBuilder builder = new StringBuilder();
        builder.append("Услуги River Park\n")
                .append("Страница ").append(pageNumber + 1);
        if (services != null && !services.isEmpty()) {
            builder.append("\n\n");
            for (ServiceItemResponseDto service : services) {
                builder.append("• ").append(service.title()).append("\n");
            }
        }
        return builder.toString().trim();
    }

    public String buildServicePrompt(List<ServiceItemResponseDto> services) {
        StringBuilder builder = new StringBuilder("Доступные услуги River Park:\n\n");
        for (ServiceItemResponseDto service : services) {
            builder.append(service.id()).append(". ").append(service.title()).append("\n");
        }
        builder.append("\nВведите номер нужной услуги.");
        return builder.toString();
    }

    public String buildServiceDetailsCaption(ServiceItemResponseDto service) {
        StringBuilder builder = new StringBuilder();
        builder.append(service.title());
        if (service.description() != null && !service.description().isBlank()) {
            builder.append("\n\n").append(service.description());
        }
        return builder.toString();
    }

    public String buildServiceDetails(ServiceItemResponseDto service) {
        StringBuilder builder = new StringBuilder();
        builder.append(service.title());
        if (service.description() != null && !service.description().isBlank()) {
            builder.append("\n\n").append(service.description());
        }
        return builder.toString();
    }

    public String buildRulesMessage(RuleDocumentResponseDto ruleDocument) {
        String fileName = ruleDocument == null || ruleDocument.fileName() == null || ruleDocument.fileName().isBlank()
                ? "river-park-rules.pdf"
                : ruleDocument.fileName();
        return "Правила проживания River Park\n\n"
                + "Файл: " + fileName + "\n"
                + "Тип: PDF\n\n"
                + "Нажмите кнопку ниже, и я отправлю PDF прямо в чат.";
    }

    public String buildSiteMessage() {
        return "Официальный сайт отеля River Park\nhttps://www.riverpark.ru";
    }

    public String buildContactsMessage(String adminContact) {
        StringBuilder builder = new StringBuilder();
        builder.append("Контакты River Park\n\n")
                .append("Бронирование:\n")
                .append("Телефон: +7 (383) 349-50-50\n")
                .append("Бесплатно по России: 8 800 200 96 66\n")
                .append("Email: bronir@riverpark.ru\n\n")
                .append("Банкеты и конференции:\n")
                .append("Телефон: +7 (383) 349-22-50\n")
                .append("Email: events@riverpark.ru\n\n")
                .append("Рестораны:\n")
                .append("Телефон: +7 (383) 349-23-50\n")
                .append("Email: bronir@riverpark.ru\n\n")
                .append("Летняя терраса:\n")
                .append("Телефон: +7 (383) 285-60-65");
        if (adminContact != null && !adminContact.isBlank()) {
            builder.append("\n\nАдминистратор: ").append(adminContact);
        }
        return builder.toString();
    }

    public String buildCatalogUnavailableMessage() {
        return "Сервис каталога сейчас недоступен. Попробуйте чуть позже.";
    }

    public String buildUnexpectedErrorMessage() {
        return "Что-то пошло не так. Попробуйте ещё раз.";
    }

    public String buildNoRoomsFoundMessage() {
        return "Номера не найдены по выбранным параметрам.";
    }

    public String buildNoRoomsAvailableForDatesMessage() {
        return "Номера не найдены на выбранные даты. Попробуйте другие даты или категорию.";
    }

    public String buildNoPhotosMessage() {
        return "Фотографии для этой категории пока недоступны.";
    }

    public String buildNegativeValueMessage() {
        return "Нельзя вводить отрицательные значения.";
    }

    public String buildPositiveGuestsMessage() {
        return "Количество гостей должно быть больше нуля.";
    }

    public String buildPositiveAdultsMessage() {
        return "Количество взрослых должно быть больше нуля.";
    }

    public String buildChildrenCountMessage() {
        return "Количество детей не может быть отрицательным.";
    }

    public String buildBookingStartMessage(RoomCategoryResponseDto room) {
        return "Бронирование номера «" + roomTypeTitle(room.name()) + "».\n\n"
                + "Введите дату заезда в формате ДД.ММ.ГГГГ.\nНапример: " + LocalDate.now().plusDays(1).format(DATE_FORMATTER);
    }

    public String buildAskBookingCheckOutMessage(LocalDate checkInDate) {
        return "Дата заезда: " + formatDate(checkInDate) + "\n\n"
                + "Теперь введите дату выезда в формате ДД.ММ.ГГГГ.";
    }

    public String buildAskBookingAdultsMessage(LocalDate checkInDate, LocalDate checkOutDate) {
        return "Период: " + formatDate(checkInDate) + " — " + formatDate(checkOutDate) + "\n\n"
                + "Введите количество взрослых.";
    }

    public String buildAskBookingChildrenMessage(Integer adults) {
        return "Взрослых: " + valueOrDash(adults) + "\n\n"
                + "Введите количество детей. Если детей нет, отправьте 0.";
    }

    public String buildAskBookingPromoMessage(Integer adults, Integer children) {
        return "Гостей: " + ((adults == null ? 0 : adults) + (children == null ? 0 : children)) + "\n\n"
                + "Если есть промокод, отправьте его. Если нет, отправьте -";
    }

    public String buildBookingCreatingMessage() {
        return "Создаю бронь и проверяю доступность номера...";
    }

    public String buildBookingCreatedMessage(BotBookingResponseDto booking, RoomCategoryResponseDto room, BotPaymentResponseDto payment, String adminContact) {
        StringBuilder builder = new StringBuilder();
        builder.append("Бронь создана и номер удержан.\n\n")
                .append("Код брони: ").append(valueOrDash(booking.bookingCode())).append("\n")
                .append("Категория: ").append(roomTypeTitle(room.name())).append("\n")
                .append("Заезд: ").append(formatDate(booking.checkInDate())).append("\n")
                .append("Выезд: ").append(formatDate(booking.checkOutDate())).append("\n")
                .append("Гостей: ").append(valueOrDash(booking.guests())).append("\n")
                .append("Сумма: ").append(resolveBookingAmount(booking, payment)).append("\n")
                .append("Статус брони: ").append(bookingStatusTitle(booking.status()));
        if (booking.holdExpiresAt() != null) {
            builder.append("\nУдержание до: ").append(formatDateTime(booking.holdExpiresAt()));
        }
        if (payment != null) {
            if (payment.paymentInstruction() != null && !payment.paymentInstruction().isBlank()) {
                builder.append("\n\nИнструкция: ").append(payment.paymentInstruction());
            }
            if (payment.contactPhone() != null && !payment.contactPhone().isBlank()) {
                builder.append("\nТелефон для связи: ").append(payment.contactPhone());
            }
            if (payment.paymentComment() != null && !payment.paymentComment().isBlank()) {
                builder.append("\nКомментарий: ").append(payment.paymentComment());
            }
        }
        if (adminContact != null && !adminContact.isBlank()) {
            builder.append("\n\nАдминистратор: ").append(adminContact);
        }
        builder.append("\n\nПодтвердите бронь кнопкой ниже или отмените её.");
        return builder.toString();
    }

    public String buildBookingProcessingMessage(BotBookingResponseDto booking) {
        return "Заявка создана и ещё обрабатывается.\n\n"
                + "Код заявки: " + valueOrDash(booking.bookingCode()) + "\n"
                + "Статус: " + bookingStatusTitle(booking.status()) + "\n\n"
                + "Она уже появится в разделе «Мои брони».";
    }

    public String buildBookingFailedMessage(BotBookingResponseDto booking) {
        if (booking != null && booking.cancellationReason() != null && booking.cancellationReason().toLowerCase(Locale.ROOT).contains("no available rooms")) {
            return buildNoRoomsAvailableForDatesMessage();
        }
        return "Не удалось оформить бронь. Попробуйте другие даты или параметры поиска.";
    }

    public String buildBookingDetails(BotBookingResponseDto booking, RoomCategoryResponseDto room, BotPaymentResponseDto payment) {
        StringBuilder builder = new StringBuilder();
        builder.append("Бронирование\n\n")
                .append("Код: ").append(valueOrDash(booking.bookingCode())).append("\n")
                .append("Статус: ").append(bookingStatusTitle(booking.status())).append("\n")
                .append("Категория: ").append(room == null ? valueOrDash(booking.roomCategoryId()) : roomTypeTitle(room.name())).append("\n")
                .append("Заезд: ").append(formatDate(booking.checkInDate())).append("\n")
                .append("Выезд: ").append(formatDate(booking.checkOutDate())).append("\n")
                .append("Гостей: ").append(valueOrDash(booking.guests())).append("\n")
                .append("Сумма: ").append(resolveBookingAmount(booking, payment));
        if (booking.holdExpiresAt() != null) {
            builder.append("\nУдержание до: ").append(formatDateTime(booking.holdExpiresAt()));
        }
        if (booking.cancellationReason() != null && !booking.cancellationReason().isBlank()) {
            builder.append("\nПричина: ").append(booking.cancellationReason());
        }
        if (payment != null) {
            builder.append("\n\nОплата\n")
                    .append("Статус: ").append(paymentStatusTitle(payment.status())).append("\n")
                    .append("Метод: ").append(valueOrDash(payment.paymentMethod()));
            if (payment.contactPhone() != null && !payment.contactPhone().isBlank()) {
                builder.append("\nТелефон: ").append(payment.contactPhone());
            }
            if (payment.paymentInstruction() != null && !payment.paymentInstruction().isBlank()) {
                builder.append("\nИнструкция: ").append(payment.paymentInstruction());
            }
            if (payment.paymentComment() != null && !payment.paymentComment().isBlank()) {
                builder.append("\nКомментарий: ").append(payment.paymentComment());
            }
        }
        return builder.toString();
    }

    public String buildMyBookingsEmptyMessage() {
        return "Активных броней пока нет.";
    }

    public String buildMyBookingsMessage(List<BotBookingResponseDto> bookings) {
        return "Ваши брони и заявки.\n\nВыберите нужную бронь по категории, дате заезда и статусу.";
    }

    public String buildBookingListLabel(int index, String roomTitle, BotBookingResponseDto booking) {
        return "Бронь " + index
                + " • " + roomTitle
                + " • " + formatShortDate(booking.checkInDate())
                + " • " + bookingStatusTitle(booking.status());
    }

    public String buildPaymentConfirmedMessage(BotBookingResponseDto booking) {
        return "Бронь подтверждена.\n\n"
                + "Код брони: " + valueOrDash(booking.bookingCode()) + "\n"
                + "Статус: " + bookingStatusTitle(booking.status());
    }

    public String buildBookingCancelledMessage(BotBookingResponseDto booking) {
        return "Бронь отменена.\n\n"
                + "Код брони: " + valueOrDash(booking.bookingCode()) + "\n"
                + "Статус: " + bookingStatusTitle(booking.status());
    }

    public String buildNotificationMessage(BotNotificationResponseDto notification) {
        StringBuilder builder = new StringBuilder();
        builder.append(notification.title() == null || notification.title().isBlank() ? "Уведомление" : notification.title());
        if (notification.createdAt() != null) {
            builder.append("\n\n").append(formatDateTime(notification.createdAt()));
        }
        if (notification.message() != null && !notification.message().isBlank()) {
            builder.append("\n\n").append(notification.message());
        }
        return builder.toString();
    }

    public String buildInvalidDateMessage() {
        return "Дата должна быть в формате ДД.ММ.ГГГГ или ГГГГ-ММ-ДД.";
    }

    public String buildPastDateMessage() {
        return "Нельзя указать дату в прошлом.";
    }

    public String buildCheckoutBeforeCheckinMessage() {
        return "Дата выезда должна быть позже даты заезда.";
    }

    public String buildGuestOverflowMessage(RoomCategoryResponseDto room) {
        return "Для категории «" + roomTypeTitle(room.name()) + "» доступно максимум " + valueOrDash(room.maxGuests()) + " гостя(ей).";
    }

    private String resolveBookingAmount(BotBookingResponseDto booking, BotPaymentResponseDto payment) {
        if (payment != null && payment.priceAmount() != null && payment.priceAmount().signum() > 0) {
            return formatPrice(payment.priceAmount());
        }
        if (booking != null && booking.priceAmount() != null && booking.priceAmount().compareTo(BigDecimal.ONE) > 0) {
            return formatPrice(booking.priceAmount());
        }
        return "Уточняется";
    }

    private String valueOrDash(Object value) {
        return value == null ? "—" : String.valueOf(value);
    }

    private String formatPrice(BigDecimal value) {
        return value == null ? "—" : decimal(value) + " ₽";
    }

    private String formatArea(Double value) {
        return value == null ? "—" : decimal(BigDecimal.valueOf(value)) + " м²";
    }

    private String decimal(BigDecimal value) {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        DecimalFormat format = new DecimalFormat("0.##", symbols);
        return format.format(value);
    }

    private String bookingStatusTitle(String status) {
        if (status == null) {
            return "—";
        }
        return switch (status.toUpperCase(Locale.ROOT)) {
            case "CREATED" -> "Обрабатывается";
            case "HOLD" -> "Удерживается";
            case "CONFIRMED" -> "Подтверждена";
            case "CANCELLED" -> "Отменена";
            case "EXPIRED" -> "Истекла";
            case "FAILED" -> "Ошибка";
            default -> status;
        };
    }

    private String paymentStatusTitle(String status) {
        if (status == null) {
            return "—";
        }
        return switch (status.toUpperCase(Locale.ROOT)) {
            case "PENDING" -> "Ожидает подтверждения";
            case "CONFIRMED" -> "Подтверждена";
            case "CANCELLED" -> "Отменена";
            case "FAILED" -> "Ошибка";
            default -> status;
        };
    }

    private String formatDate(LocalDate date) {
        return date == null ? "—" : date.format(DATE_FORMATTER);
    }

    private String formatShortDate(LocalDate date) {
        return date == null ? "—" : date.format(SHORT_DATE_FORMATTER);
    }

    private String formatDateTime(OffsetDateTime dateTime) {
        return dateTime == null ? "—" : dateTime.toLocalDateTime().format(DATE_TIME_FORMATTER);
    }

    private String roomTypeTitle(RoomType roomType) {
        if (roomType == null) {
            return "Номер";
        }
        return switch (roomType) {
            case STANDARD -> "Standard";
            case STANDARD_DOUBLE -> "Standard Double";
            case STANDARD_PLUS -> "Standard Plus";
            case STUDIO -> "Studio";
            case BUSINESS_STUDIO -> "Business Studio";
            case ECONOMY -> "Economy";
        };
    }
}
