package ru.haritonenko.telegrambot.bot.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.haritonenko.commonlibs.dto.category.RoomCategoryResponseDto;
import ru.haritonenko.commonlibs.dto.category.RoomCategorySearchRequestDto;
import ru.haritonenko.commonlibs.dto.category.type.RoomType;
import ru.haritonenko.commonlibs.dto.rule.RuleDocumentResponseDto;
import ru.haritonenko.commonlibs.dto.service.ServiceItemResponseDto;
import ru.haritonenko.telegrambot.config.BotMessagesProperties;
import ru.haritonenko.telegrambot.bot.state.AvailableRoomSearchDraft;
import ru.haritonenko.telegrambot.dto.booking.BotBookingResponseDto;
import ru.haritonenko.telegrambot.dto.booking.BotTariffResponseDto;
import ru.haritonenko.telegrambot.dto.notification.BotNotificationResponseDto;
import ru.haritonenko.telegrambot.dto.payment.BotPaymentResponseDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class BotTextFactory {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final DateTimeFormatter SHORT_DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM");
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final ZoneId NOVOSIBIRSK_ZONE = ZoneId.of("Asia/Novosibirsk");
    private static final String DEFAULT_PAYMENT_INSTRUCTION = "Подтвердите бронирование в Telegram. Оплата производится в день заселения у администратора.";
    private static final String DEFAULT_PAYMENT_COMMENT = "Оплата производится в день заселения у администратора отеля River Park.";
    private final BotMessagesProperties messages;

    public String buildStartMessage() {
        return "Добро пожаловать в River Park Hotel.\n\n"
                + "Я помогу подобрать номер, показать фото, оформить бронь и напомнить о важных статусах.\n\n"
                + "Основные разделы:\n"
                + "• Подобрать номер — поиск по гостям, датам, цене и площади\n"
                + "• Все номера — каталог с фото и подробностями\n"
                + "• Мои брони — актуальные бронирования и история\n"
                + "• Услуги — дополнительные возможности отеля\n\n"
                + "Команды:\n"
                + "/start — главное меню\n"
                + "/help — помощь по боту\n"
                + "/site — официальный сайт отеля";
    }

    public String buildMenuMessage() {
        return "Выберите нужный раздел ниже.";
    }

    public String buildFilterStartMessage(int maxGuests, int maxAdults, int maxChildren) {
        return "Подбор номера.\n\n"
                + "Введите количество гостей числом или отправьте -, если количество не важно.\n"
                + "Максимум: " + maxGuests + " гостей, до " + maxAdults + " взрослых и до " + maxChildren + " детей.\n"
                + "Например: 2";
    }

    public String buildMaxGuestsMessage(int maxGuests, int maxAdults, int maxChildren) {
        return messages.booking().maxGuests().formatted(maxGuests, maxAdults, maxChildren);
    }

    public String buildFilterMenuMessage(AvailableRoomSearchDraft draft, int maxGuests, int maxAdults, int maxChildren) {
        AvailableRoomSearchDraft safeDraft = draft == null ? AvailableRoomSearchDraft.empty() : draft;
        return "Подбор номера.\n\n"
                + "Выберите нужные фильтры кнопками ниже. Можно заполнить их в любом порядке и нажать «Найти» после любого выбранного параметра.\n"
                + "Максимум: " + maxGuests + " гостей, до " + maxAdults + " взрослых и до " + maxChildren + " детей.\n\n"
                + "Текущие фильтры:\n"
                + "Гости: " + readableGuestComposition(safeDraft) + "\n"
                + "Даты: " + readableDateRangeOrDash(safeDraft.checkInDate(), safeDraft.checkOutDate()) + "\n"
                + "Тип номера: " + (safeDraft.roomType() == null ? "любой" : roomTypeTitle(safeDraft.roomType())) + "\n"
                + "Цена: " + readablePriceRangeOrDash(safeDraft.priceFrom(), safeDraft.priceTo()) + "\n"
                + "Площадь от: " + readableValueOrDash(safeDraft.minArea());
    }

    public String buildAskFilterGuestsMessage(int maxGuests) {
        return "Введите количество гостей числом от 1 до " + maxGuests + ".";
    }

    public String buildAskFilterCheckInButtonMessage() {
        return "Введите дату заезда в формате ДД.ММ.ГГГГ.\n"
                + "Самая ранняя дата: " + LocalDate.now(NOVOSIBIRSK_ZONE).format(DATE_FORMATTER);
    }

    public String buildAskFilterCheckOutButtonMessage(LocalDate checkInDate) {
        return "Дата заезда: " + formatDate(checkInDate) + "\n\n"
                + "Введите дату выезда в формате ДД.ММ.ГГГГ.";
    }

    public String buildAskRoomTypeButtonMessage() {
        return "Выберите тип номера кнопками ниже.";
    }

    public String buildAskPriceFromButtonMessage() {
        return "Введите минимальную цену за ночь.";
    }

    public String buildAskPriceBoundaryButtonMessage() {
        return "Выберите, какую границу цены изменить.";
    }

    public String buildAskPriceToButtonMessage() {
        return "Введите максимальную цену за ночь.";
    }

    public String buildAskMinAreaButtonMessage() {
        return "Введите минимальную площадь номера.";
    }

    public String buildBookingAlreadyInactiveMessage() {
        return messages.validation().bookingAlreadyInactive();
    }

    public String buildValidationDateErrorMessage() {
        return messages.validation().dateError();
    }

    public String buildValidationFilterErrorMessage() {
        return messages.validation().filterError();
    }

    public String buildPriceRangeMessage(BigDecimal minPrice, BigDecimal maxPrice) {
        return messages.validation().priceRange().formatted(
                minPrice.stripTrailingZeros().toPlainString(),
                maxPrice.stripTrailingZeros().toPlainString()
        );
    }

    public String buildAreaRangeMessage(BigDecimal minArea, BigDecimal maxArea) {
        return messages.validation().areaRange().formatted(
                minArea.stripTrailingZeros().toPlainString(),
                maxArea.stripTrailingZeros().toPlainString()
        );
    }

    public String buildValidationDefaultErrorMessage() {
        return messages.validation().defaultError();
    }

    public String buildPromoCodeIgnoredMessage() {
        return messages.validation().promoIgnored();
    }

    public String buildPromoCodeIgnoredPrefixMessage(String message) {
        return messages.validation().promoIgnored() + "\n\n" + message;
    }

    public String buildBookingServiceUnavailableMessage() {
        return messages.serviceUnavailable().booking();
    }

    public String buildCatalogServiceUnavailableMessage() {
        return messages.serviceUnavailable().catalog();
    }

    public String buildPaymentServiceUnavailableMessage() {
        return messages.serviceUnavailable().payment();
    }

    public String buildNotificationServiceUnavailableMessage() {
        return messages.serviceUnavailable().notification();
    }

    public String buildUserServiceUnavailableMessage() {
        return messages.serviceUnavailable().user();
    }

    public String buildDefaultServiceUnavailableMessage() {
        return messages.serviceUnavailable().defaultMessage();
    }

    public String buildNotificationsAutomaticInfoMessage() {
        return messages.notification().automaticInfo();
    }

    public String buildRoomIdMustBeNumberMessage() {
        return messages.input().roomIdMustBeNumber();
    }

    public String buildServiceIdMustBeNumberMessage() {
        return messages.input().serviceIdMustBeNumber();
    }

    public String buildGuestsMustBeIntegerMessage() {
        return messages.input().guestsMustBeInteger();
    }

    public String buildRoomTypeNotRecognizedMessage() {
        return messages.input().roomTypeNotRecognized();
    }

    public String buildMaxPriceLowerThanMinMessage() {
        return messages.input().maxPriceLowerThanMin();
    }

    public String buildMinPriceMustBeNumberMessage() {
        return messages.input().minPriceMustBeNumber();
    }

    public String buildMaxPriceMustBeNumberMessage() {
        return messages.input().maxPriceMustBeNumber();
    }

    public String buildMinAreaMustBeNumberMessage() {
        return messages.input().minAreaMustBeNumber();
    }

    public String buildBookingSelectedPeriodMessage(LocalDate checkInDate, LocalDate checkOutDate, String availableUnits) {
        return normalizeLineBreaks(messages.booking().selectedPeriod().formatted(
                checkInDate.format(DATE_FORMATTER),
                checkOutDate.format(DATE_FORMATTER),
                availableUnits
        ));
    }

    public String buildAdultsMustBeIntegerMessage() {
        return messages.input().adultsMustBeInteger();
    }

    public String buildChildrenMustBeIntegerMessage() {
        return messages.input().childrenMustBeInteger();
    }

    public String buildCancellingBookingMessage() {
        return messages.booking().cancelling();
    }

    public String buildInactiveBookingsEmptyMessage() {
        return messages.booking().inactiveEmpty();
    }

    public String buildInactiveBookingsTitleMessage() {
        return messages.booking().inactiveTitle();
    }

    public String buildEarlyBookingsEmptyMessage() {
        return messages.booking().earlyEmpty();
    }

    public String buildEarlyBookingsTitleMessage() {
        return messages.booking().earlyTitle();
    }

    public String buildBookingHistoryEmptyMessage() {
        return messages.booking().historyEmpty();
    }

    public String buildBookingHistoryTitleMessage() {
        return messages.booking().historyTitle();
    }

    public String buildRoomDefaultTitle() {
        return messages.booking().roomDefaultTitle();
    }

    public String buildNoUnreadNotificationsMessage() {
        return messages.notification().noneUnread();
    }

    public String buildNotificationMarkedReadMessage() {
        return messages.notification().markedRead();
    }

    public String buildAllNotificationsMarkedReadMessage() {
        return messages.notification().allMarkedRead();
    }

    private String normalizeLineBreaks(String value) {
        return value.replace("\\n", "\n");
    }

    public String buildAskFilterCheckInMessage() {
        return "Введите дату заезда в формате ДД.ММ.ГГГГ или отправьте -, если даты не важны.\n"
                + "Самая ранняя дата: " + LocalDate.now(NOVOSIBIRSK_ZONE).format(DATE_FORMATTER);
    }

    public String buildAskFilterCheckOutMessage(LocalDate checkInDate) {
        return "Дата заезда: " + formatDate(checkInDate) + "\n\n"
                + "Введите дату выезда в формате ДД.ММ.ГГГГ или отправьте -, чтобы искать без дат.";
    }

    public String buildAskRoomTypeMessage() {
        return "Выберите тип номера кнопками ниже.\n\nМожно выбрать конкретный тип или продолжить без него.";
    }

    public String buildAskPriceFromMessage() {
        return "Введите минимальную цену за ночь от 5000 до 20000. Если фильтр не нужен, отправьте -";
    }

    public String buildAskPriceToMessage() {
        return "Введите максимальную цену за ночь от 5000 до 20000. Если фильтр не нужен, отправьте -";
    }

    public String buildAskMinAreaMessage() {
        return "Введите минимальную площадь от 10 до 60 м². Если фильтр не нужен, отправьте -";
    }

    public String buildFilterSummary(RoomCategorySearchRequestDto filter) {
        return "Параметры подбора:\n\n"
                + "Гости: " + valueOrDash(filter.guests()) + "\n"
                + "Тип номера: " + (filter.roomType() == null ? "Любой" : roomTypeTitle(filter.roomType())) + "\n"
                + "Цена от: " + valueOrDash(filter.priceFrom()) + "\n"
                + "Цена до: " + valueOrDash(filter.priceTo()) + "\n"
                + "Мин. площадь: " + valueOrDash(filter.minArea());
    }

    public String buildRoomSelectionMessage(List<RoomCategoryResponseDto> rooms) {
        StringBuilder builder = new StringBuilder("Доступные номера River Park:\n\n");
        for (RoomCategoryResponseDto room : rooms) {
            builder.append("• ")
                    .append(roomTypeTitle(room.name()))
                    .append("\n");
        }
        builder.append("\nОткройте карточку нужного номера кнопками в каталоге.");
        return builder.toString();
    }

    public String buildRoomCard(RoomCategoryResponseDto room, int pageNumber, int totalPages) {
        return buildRoomCard(room, pageNumber, totalPages, false);
    }

    public String buildRoomCard(RoomCategoryResponseDto room, int pageNumber, int totalPages, boolean filtered) {
        return buildRoomCard(room, pageNumber, totalPages, filtered, null, null);
    }

    public String buildRoomCard(
            RoomCategoryResponseDto room,
            int pageNumber,
            int totalPages,
            boolean filtered,
            LocalDate checkInDate,
            LocalDate checkOutDate
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append(filtered ? "Подходящие номера River Park\n" : "Номера River Park\n")
                .append("Страница ")
                .append(pageNumber + 1)
                .append(" из ")
                .append(Math.max(totalPages, 1))
                .append("\n");
        builder.append("\n")
                .append(roomTypeTitle(room.name()))
                .append("\n")
                .append("Гостей: до ").append(valueOrDash(room.maxGuests())).append("\n")
                .append("Цена: ").append(formatPrice(room.basePrice())).append(" за ночь\n")
                .append("Площадь: ").append(formatArea(room.areaSquare())).append("\n")
                .append("Всего номеров: ").append(valueOrDash(room.totalUnits())).append("\n")
                .append("Свободно номеров: ").append(valueOrDash(availableUnits(room)));
        return builder.toString();
    }

    public String buildRoomDetails(RoomCategoryResponseDto room) {
        StringBuilder builder = new StringBuilder();
        builder.append(roomTypeTitle(room.name())).append("\n")
                .append("Гостей: до ").append(valueOrDash(room.maxGuests())).append("\n")
                .append("Цена за ночь: ").append(formatPrice(room.basePrice())).append("\n")
                .append("Площадь: ").append(formatArea(room.areaSquare())).append("\n")
                .append("Всего номеров: ").append(valueOrDash(room.totalUnits())).append("\n")
                .append("Свободно номеров: ").append(valueOrDash(availableUnits(room)));
        if (room.description() != null && !room.description().isBlank()) {
            builder.append("\n\n").append(room.description());
        }
        return builder.toString();
    }

    public String buildAvailableRoomDetails(RoomCategoryResponseDto room) {
        StringBuilder builder = new StringBuilder();
        builder.append(roomTypeTitle(room.name())).append("\n")
                .append("Гостей: до ").append(valueOrDash(room.maxGuests())).append("\n")
                .append("Цена за ночь: ").append(formatPrice(room.basePrice())).append("\n")
                .append("Площадь: ").append(formatArea(room.areaSquare())).append("\n")
                .append("Всего номеров: ").append(valueOrDash(room.totalUnits())).append("\n")
                .append("Свободно номеров: ").append(valueOrDash(availableUnits(room)));
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
        return "Номера не найдены на выбранные даты. Попробуйте другие даты или тип номера.";
    }

    public String buildNoPhotosMessage() {
        return "Фотографии для этого номера пока недоступны.";
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
                + "Введите дату заезда в формате ДД.ММ.ГГГГ.\nНапример: " + LocalDate.now(NOVOSIBIRSK_ZONE).plusDays(1).format(DATE_FORMATTER);
    }

    public String buildAskBookingCheckOutMessage(LocalDate checkInDate) {
        return "Дата заезда: " + formatDate(checkInDate) + "\n\n"
                + "Теперь введите дату выезда в формате ДД.ММ.ГГГГ.";
    }

    public String buildAskBookingAdultsMessage(LocalDate checkInDate, LocalDate checkOutDate, int maxAdults, int maxGuests) {
        return "Введите количество взрослых.\n"
                + "Максимум: " + maxAdults + " взрослых, всего до " + maxGuests + " гостей.";
    }

    public String buildAskBookingChildrenMessage(Integer adults, int maxChildren, int maxGuests) {
        return "Взрослых: " + valueOrDash(adults) + "\n\n"
                + "Введите количество детей. Если детей нет, отправьте 0.\n"
                + "Максимум: " + maxChildren + " детей, всего до " + maxGuests + " гостей.";
    }

    public String buildAskBookingPromoMessage(Integer adults, Integer children) {
        return "Гостей: " + ((adults == null ? 0 : adults) + (children == null ? 0 : children)) + "\n"
                + "Взрослых: " + valueOrDash(adults) + "\n"
                + "Детей: " + valueOrDash(children) + "\n\n"
                + "Если есть промокод, отправьте его. Если нет, отправьте -";
    }

    public String buildTariffSelectionMessage(List<BotTariffResponseDto> tariffs) {
        StringBuilder builder = new StringBuilder("Выберите тариф для бронирования.\n\n");
        for (BotTariffResponseDto tariff : tariffs) {
            builder.append(tariff.title()).append(" — ").append(formatPrice(tariff.priceAmount())).append("\n");
            if (tariff.includedServices() != null && !tariff.includedServices().isBlank()) {
                builder.append("Включено: ").append(tariff.includedServices()).append("\n");
            }
            builder.append("Отмена: ").append(tariffCancellationPolicyTitle(tariff)).append("\n\n");
        }
        return builder.toString().trim();
    }

    public String buildBookingCreatingMessage() {
        return "Создаю бронь и проверяю доступность номера...";
    }

    public String buildBookingCreatedMessage(BotBookingResponseDto booking, RoomCategoryResponseDto room, BotPaymentResponseDto payment, String adminContact) {
        StringBuilder builder = new StringBuilder();
        builder.append("Бронь создана и номер удержан.\n\n")
                .append("Код брони: ").append(valueOrDash(booking.bookingCode())).append("\n")
                .append("Номер: ").append(roomTypeTitle(room.name())).append("\n")
                .append("Тариф: ").append(valueOrDash(booking.tariffTitle())).append("\n")
                .append("Заезд: ").append(formatDate(booking.checkInDate())).append("\n")
                .append("Выезд: ").append(formatDate(booking.checkOutDate())).append("\n")
                .append("Гостей: ").append(valueOrDash(booking.guests())).append("\n")
                .append("Взрослых: ").append(valueOrDash(booking.adultCount())).append("\n")
                .append("Детей: ").append(valueOrDash(booking.childrenCount())).append("\n")
                .append("Сумма: ").append(resolveBookingAmount(booking, payment)).append("\n")
                .append("Статус брони: ").append(bookingStatusTitle(booking.status()));
        if (isHoldStatus(booking.status()) && booking.holdExpiresAt() != null) {
            builder.append("\nУдержание до: ").append(formatDateTime(booking.holdExpiresAt()));
        }
        if (booking.generatedPromoCode() != null && !booking.generatedPromoCode().isBlank()) {
            builder.append("\n")
                    .append(messages.booking().nextPromoCode())
                    .append(booking.generatedPromoCode());
            if (booking.promoDiscountPercent() != null) {
                builder.append(" (-").append(booking.promoDiscountPercent()).append("%)");
            }
        }
        if (payment != null) {
            if (shouldShowPaymentInstruction(payment)) {
                builder.append("\n\nИнструкция: ").append(readablePaymentInstruction(payment));
            }
            if (payment.contactPhone() != null && !payment.contactPhone().isBlank()) {
                builder.append("\nТелефон для связи: ").append(payment.contactPhone());
            }
            if (shouldShowPaymentComment(payment)) {
                builder.append("\nПримечание: ").append(readablePaymentComment(payment));
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
                .append("Номер: ").append(room == null ? messages.booking().roomDefaultTitle() : roomTypeTitle(room.name())).append("\n")
                .append("Тариф: ").append(valueOrDash(booking.tariffTitle())).append("\n")
                .append("Заезд: ").append(formatDate(booking.checkInDate())).append("\n")
                .append("Выезд: ").append(formatDate(booking.checkOutDate())).append("\n")
                .append("Гостей: ").append(valueOrDash(booking.guests())).append("\n")
                .append("Взрослых: ").append(valueOrDash(booking.adultCount())).append("\n")
                .append("Детей: ").append(valueOrDash(booking.childrenCount())).append("\n")
                .append("Сумма: ").append(resolveBookingAmount(booking, payment));
        if (isHoldStatus(booking.status()) && booking.holdExpiresAt() != null) {
            builder.append("\nУдержание до: ").append(formatDateTime(booking.holdExpiresAt()));
        }
        if (booking.cancellationReason() != null
                && !booking.cancellationReason().isBlank()
                && !isUserCancellationReason(booking.cancellationReason())) {
            builder.append("\nПричина: ").append(cancellationReasonTitle(booking.cancellationReason()));
        }
        if (payment != null) {
            builder.append("\n\nОплата\n")
                    .append("Статус: ").append(paymentStatusTitle(payment.status())).append("\n")
                    .append("Метод: ").append(paymentMethodTitle(payment.paymentMethod()));
            if (payment.contactPhone() != null && !payment.contactPhone().isBlank()) {
                builder.append("\nТелефон: ").append(payment.contactPhone());
            }
            if (shouldShowPaymentInstruction(payment)) {
                builder.append("\nИнструкция: ").append(readablePaymentInstruction(payment));
            }
            if (shouldShowPaymentComment(payment)) {
                builder.append("\nПримечание: ").append(readablePaymentComment(payment));
            }
        }
        return builder.toString();
    }

    public String buildMyBookingsEmptyMessage() {
        return "Активных броней пока нет.";
    }

    public String buildMyBookingsMessage(List<BotBookingResponseDto> bookings) {
        return "Ваши брони и заявки.\n\nВыберите нужную бронь по номеру, дате заезда и статусу.";
    }

    public String buildBookingListLabel(int index, String roomTitle, BotBookingResponseDto booking) {
        String label = "Бронь " + index
                + " • " + roomTitle
                + " • " + formatShortDate(booking.checkInDate())
                + " • " + bookingStatusTitle(booking.status());
        if (isInactiveBookingStatus(booking.status())) {
            label += " • " + inactiveReasonTitle(booking);
        }
        return label;
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
        return "Нельзя указать дату в прошлом. Самая ранняя дата: " + LocalDate.now(NOVOSIBIRSK_ZONE).format(DATE_FORMATTER);
    }

    public String buildCheckoutBeforeCheckinMessage() {
        return "Дата выезда должна быть позже даты заезда.";
    }

    public String buildGuestOverflowMessage(RoomCategoryResponseDto room) {
        return "Для номера «" + roomTypeTitle(room.name()) + "» доступно максимум " + valueOrDash(room.maxGuests()) + " гостя(ей).";
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

    private String readableValueOrDash(Object value) {
        return value == null ? "—" : String.valueOf(value);
    }

    private String readableGuestComposition(AvailableRoomSearchDraft draft) {
        if (draft.guests() == null && draft.adultCount() == null && draft.childrenCount() == null) {
            return readableValueOrDash(null);
        }
        return readableValueOrDash(draft.guests())
                + " (взрослых: " + readableValueOrDash(draft.adultCount())
                + ", детей: " + readableValueOrDash(draft.childrenCount()) + ")";
    }

    private String readableDateRangeOrDash(LocalDate checkInDate, LocalDate checkOutDate) {
        if (checkInDate == null || checkOutDate == null) {
            return "—";
        }
        return formatDate(checkInDate) + " - " + formatDate(checkOutDate);
    }

    private String readablePriceRangeOrDash(BigDecimal priceFrom, BigDecimal priceTo) {
        if (priceFrom == null && priceTo == null) {
            return "—";
        }
        return readableValueOrDash(priceFrom) + " - " + readableValueOrDash(priceTo);
    }

    private String tariffCancellationPolicyTitle(BotTariffResponseDto tariff) {
        if (tariff == null || tariff.cancellationPolicy() == null) {
            return "условия уточняются";
        }
        return switch (tariff.cancellationPolicy().toUpperCase(Locale.ROOT)) {
            case "NON_REFUNDABLE" -> "невозвратный тариф";
            case "FREE_UNTIL_DEADLINE" -> "бесплатно до " + valueOrDash(tariff.freeCancellationDaysBefore()) + " дн. до заезда";
            case "FLEXIBLE" -> "гибкая отмена";
            case "STRICT" -> "строгие условия";
            default -> tariff.cancellationPolicy();
        };
    }

    private Integer availableUnits(RoomCategoryResponseDto room) {
        return room.availableUnits() == null ? room.totalUnits() : room.availableUnits();
    }

    private String formatPrice(BigDecimal value) {
        return value == null ? "—" : decimal(value) + " ₽";
    }

    private String formatArea(Double value) {
        return value == null ? "—" : decimal(BigDecimal.valueOf(value)) + " м²";
    }

    private String decimal(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
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

    private String paymentMethodTitle(String method) {
        if (method == null || method.isBlank()) {
            return "—";
        }
        return switch (method.toUpperCase(Locale.ROOT)) {
            case "PAY_ON_ARRIVAL" -> "Оплата при заселении";
            default -> method;
        };
    }

    private boolean shouldShowPaymentInstruction(BotPaymentResponseDto payment) {
        return payment != null
                && payment.paymentInstruction() != null
                && !payment.paymentInstruction().isBlank()
                && "PENDING".equalsIgnoreCase(payment.status());
    }

    private boolean shouldShowPaymentComment(BotPaymentResponseDto payment) {
        if (payment == null
                || payment.paymentComment() == null
                || payment.paymentComment().isBlank()
                || isInactivePaymentStatus(payment.status())) {
            return false;
        }
        String instruction = payment.paymentInstruction() == null ? "" : payment.paymentInstruction().trim().toLowerCase(Locale.ROOT);
        String comment = payment.paymentComment().trim().toLowerCase(Locale.ROOT);
        return !instruction.contains(comment)
                && !(instruction.contains("оплата производится") && comment.contains("оплата производится"));
    }

    private String readablePaymentInstruction(BotPaymentResponseDto payment) {
        return readablePaymentText(payment.paymentInstruction(), DEFAULT_PAYMENT_INSTRUCTION);
    }

    private String readablePaymentComment(BotPaymentResponseDto payment) {
        return readablePaymentText(payment.paymentComment(), DEFAULT_PAYMENT_COMMENT);
    }

    private String readablePaymentText(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        String trimmed = value.trim();
        if (looksCorruptedText(trimmed)) {
            return fallback;
        }
        return trimmed;
    }

    private boolean looksCorruptedText(String value) {
        long replacementChars = value.chars().filter(ch -> ch == '\uFFFD').count();
        return replacementChars >= 3 || value.contains("Р�") || value.contains("Рџ") || value.contains("Рћ");
    }

    private boolean isInactivePaymentStatus(String status) {
        return status != null && List.of("CANCELLED", "FAILED").contains(status.toUpperCase(Locale.ROOT));
    }

    private boolean isHoldStatus(String status) {
        return status != null && "HOLD".equalsIgnoreCase(status);
    }

    private boolean isInactiveBookingStatus(String status) {
        return status != null && List.of("CANCELLED", "EXPIRED", "FAILED").contains(status.toUpperCase(Locale.ROOT));
    }

    private String inactiveReasonTitle(BotBookingResponseDto booking) {
        if (booking == null || booking.status() == null) {
            return "Причина не указана";
        }
        String status = booking.status().toUpperCase(Locale.ROOT);
        if ("CANCELLED".equals(status) && isUserCancellationReason(booking.cancellationReason())) {
            return "Отменено пользователем";
        }
        if ("EXPIRED".equals(status) && (booking.cancellationReason() == null || booking.cancellationReason().isBlank())) {
            return "Истекло время";
        }
        if (booking.cancellationReason() == null || booking.cancellationReason().isBlank()) {
            return "Причина не указана";
        }
        return cancellationReasonTitle(booking.cancellationReason());
    }

    private boolean isUserCancellationReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return false;
        }
        String normalized = reason.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("отменено")
                || normalized.startsWith("cancelled by user")
                || normalized.startsWith("canceled by user");
    }

    private String cancellationReasonTitle(String reason) {
        if (reason == null || reason.isBlank()) {
            return "—";
        }
        String normalized = reason.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("cancelled by user") || normalized.startsWith("canceled by user")) {
            return "Отменено";
        }
        if (normalized.contains("hold expired")) {
            return "Истекло время удержания";
        }
        if (normalized.contains("booking processing timed out")) {
            return "Истекло время обработки";
        }
        if (normalized.contains("no available rooms")) {
            return "Нет свободных номеров на выбранные даты";
        }
        return reason;
    }

    private String formatDate(LocalDate date) {
        return date == null ? "—" : date.format(DATE_FORMATTER);
    }

    private String formatShortDate(LocalDate date) {
        return date == null ? "—" : date.format(SHORT_DATE_FORMATTER);
    }

    private String formatDateTime(OffsetDateTime dateTime) {
        return dateTime == null ? "—" : dateTime.atZoneSameInstant(NOVOSIBIRSK_ZONE).format(DATE_TIME_FORMATTER);
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
