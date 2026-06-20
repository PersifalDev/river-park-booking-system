package ru.haritonenko.telegrambot.bot.service.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.haritonenko.commonlibs.dto.category.RoomCategoryResponseDto;
import ru.haritonenko.commonlibs.dto.category.RoomCategorySearchRequestDto;
import ru.haritonenko.commonlibs.dto.photo.RoomCategoryPhotoResponseDto;
import ru.haritonenko.commonlibs.dto.service.ServiceItemResponseDto;
import ru.haritonenko.commonlibs.utils.pages.PageResponse;
import ru.haritonenko.telegrambot.bot.service.storage.BotConversationStore;
import ru.haritonenko.telegrambot.bot.state.AvailableRoomSearchDraft;
import ru.haritonenko.telegrambot.bot.state.ChatStateService;
import ru.haritonenko.telegrambot.bot.util.BotKeyboardFactory;
import ru.haritonenko.telegrambot.bot.util.BotTextFactory;
import ru.haritonenko.telegrambot.client.BookingClient;
import ru.haritonenko.telegrambot.client.CatalogClient;
import ru.haritonenko.telegrambot.config.BotFlowProperties;
import ru.haritonenko.telegrambot.dto.booking.BotAvailableRoomSearchRequestDto;
import ru.haritonenko.telegrambot.service.BotMessageService;
import ru.haritonenko.telegrambot.service.auth.BotAuthService;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
public class CatalogFlowHandler {

    private static final ZoneId NOVOSIBIRSK_ZONE = ZoneId.of("Asia/Novosibirsk");

    private final CatalogClient catalogClient;
    private final BookingClient bookingClient;
    private final BotAuthService botAuthService;
    private final BotMessageService botMessageService;
    private final BotKeyboardFactory botKeyboardFactory;
    private final BotTextFactory botTextFactory;
    private final ChatStateService chatStateService;
    private final BotFlowProperties botFlowProperties;
    private final BotConversationStore conversationStore;

    public void handleRoomViewCallback(Long chatId, Integer messageId, String data, boolean photoMessage) {
        String[] parts = data.split(":");
        Long roomId = Long.parseLong(parts[2]);
        int pageNumber = parts.length > 3 ? Integer.parseInt(parts[3]) : 0;
        RoomCategoryResponseDto room = resolveRoomForDisplay(chatId, roomId);
        boolean roomFromAvailableSearch = conversationStore.hasAvailableRoom(chatId, roomId);
        boolean filtered = parts.length > 4 && "filter".equalsIgnoreCase(parts[4]);
        String details = roomFromAvailableSearch
                ? botTextFactory.buildAvailableRoomDetails(room)
                : botTextFactory.buildRoomDetails(room);

        if (photoMessage) {
            botMessageService.deleteMessage(chatId, messageId);
            botMessageService.sendText(chatId, details, botKeyboardFactory.roomDetails(roomId, pageNumber, filtered));
            return;
        }

        botMessageService.editText(chatId, messageId, details, botKeyboardFactory.roomDetails(roomId, pageNumber, filtered));
    }

    public void handlePhotoIndexCallback(Long chatId, Integer messageId, String data) {
        String[] parts = data.split(":");
        Long roomId = Long.parseLong(parts[3]);
        int photoIndex = Integer.parseInt(parts[4]);
        int roomPageNumber = Integer.parseInt(parts[5]);
        boolean filtered = parts.length > 6 && "filter".equalsIgnoreCase(parts[6]);
        showPhotoPage(chatId, messageId, roomId, photoIndex, roomPageNumber, filtered);
    }

    public void handleServiceViewCallback(Long chatId, Integer messageId, String data) {
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

    public void handleRoomId(Long chatId, String text) {
        Long roomId = parseLong(text, botTextFactory.buildRoomIdMustBeNumberMessage(), chatId);
        if (roomId == null) {
            return;
        }

        RoomCategoryResponseDto room = catalogClient.getRoomById(roomId);
        chatStateService.reset(chatId);
        botMessageService.sendText(chatId, botTextFactory.buildRoomDetails(room), botKeyboardFactory.roomDetails(roomId, 0));
    }

    public void handleServiceId(Long chatId, String text) {
        Long serviceId = parseLong(text, botTextFactory.buildServiceIdMustBeNumberMessage(), chatId);
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

    public void sendRoomsPage(Long chatId, int pageNumber, Integer messageId, RoomCategorySearchRequestDto filter) {
        sendRoomsPage(chatId, pageNumber, messageId, filter, false);
    }

    public void sendRoomsPage(Long chatId, int pageNumber, Integer messageId, RoomCategorySearchRequestDto filter, boolean useCurrentFilter) {
        AvailableRoomSearchDraft draft = useCurrentFilter ? chatStateService.get(chatId).availableRoomSearchDraft() : displayAvailabilityDraft(chatId);
        boolean hasDateFilter = useCurrentFilter && draft != null && draft.checkInDate() != null && draft.checkOutDate() != null;
        boolean hasCatalogFilter = useCurrentFilter && hasCatalogFilter(draft);
        AvailableRoomSearchDraft availabilityDisplayDraft = hasDateFilter ? draft : null;

        PageResponse<RoomCategoryResponseDto> page;
        if (hasDateFilter) {
            page = bookingClient.searchAvailableRooms(
                    botAuthService.getJwt(chatId),
                    new BotAvailableRoomSearchRequestDto(
                            draft.checkInDate(),
                            draft.checkOutDate(),
                            draft.guests(),
                            draft.adultCount(),
                            draft.childrenCount(),
                            draft.roomType(),
                            draft.priceFrom(),
                            draft.priceTo(),
                            draft.minArea()
                    ),
                    pageNumber,
                    botFlowProperties.pagination().roomPageSize()
            );
            cacheAvailableRooms(chatId, page);
        } else if (hasCatalogFilter) {
            conversationStore.removeAvailableRooms(chatId);
            filter = toCatalogFilter(draft);
            page = catalogClient.searchRooms(filter, pageNumber, botFlowProperties.pagination().roomPageSize());
        } else {
            if (!useCurrentFilter) {
                if (filter == null) {
                    conversationStore.removeAvailableRooms(chatId);
                    page = catalogClient.getRooms(pageNumber, botFlowProperties.pagination().roomPageSize());
                } else {
                    conversationStore.removeAvailableRooms(chatId);
                    page = catalogClient.searchRooms(filter, pageNumber, botFlowProperties.pagination().roomPageSize());
                }
            } else {
                conversationStore.removeAvailableRooms(chatId);
                page = catalogClient.getRooms(pageNumber, botFlowProperties.pagination().roomPageSize());
            }
        }

        if (page.content() == null || page.content().isEmpty()) {
            botMessageService.sendText(chatId, botTextFactory.buildNoRoomsFoundMessage(), botKeyboardFactory.mainMenu());
            return;
        }

        RoomCategoryResponseDto room = page.content().get(0);
        if (hasDateFilter) {
            conversationStore.putAvailableRoom(chatId, room);
        }
        String caption = botTextFactory.buildRoomCard(
                room,
                page.number(),
                page.totalPages(),
                hasDateFilter || filter != null,
                availabilityDisplayDraft == null ? null : availabilityDisplayDraft.checkInDate(),
                availabilityDisplayDraft == null ? null : availabilityDisplayDraft.checkOutDate()
        );
        String pagePrefix = useCurrentFilter ? "rooms:filter:page:" : "rooms:page:";

        if (messageId == null) {
            botMessageService.sendPhoto(chatId, room.mainPhotoUrl(), caption, botKeyboardFactory.roomCard(room.id(), page.number(), page.totalPages(), pagePrefix));
            return;
        }

        if (!botMessageService.editPhoto(chatId, messageId, room.mainPhotoUrl(), caption, botKeyboardFactory.roomCard(room.id(), page.number(), page.totalPages(), pagePrefix))) {
            botMessageService.deleteMessage(chatId, messageId);
            botMessageService.sendPhoto(chatId, room.mainPhotoUrl(), caption, botKeyboardFactory.roomCard(room.id(), page.number(), page.totalPages(), pagePrefix));
        }
    }

    public void sendServicesPage(Long chatId, int pageNumber, Integer messageId) {
        List<ServiceItemResponseDto> services = catalogClient.getServices(pageNumber, botFlowProperties.pagination().servicePageSize());
        List<ServiceItemResponseDto> nextServices = catalogClient.getServices(pageNumber + 1, botFlowProperties.pagination().servicePageSize());
        String text = botTextFactory.buildServicesMessage(services, pageNumber);

        if (messageId == null) {
            botMessageService.sendText(chatId, text, botKeyboardFactory.servicesPage(services, pageNumber, !nextServices.isEmpty()));
            return;
        }

        botMessageService.editText(chatId, messageId, text, botKeyboardFactory.servicesPage(services, pageNumber, !nextServices.isEmpty()));
    }

    public void sendRules(Long chatId) {
        botMessageService.sendText(chatId, botTextFactory.buildRulesMessage(catalogClient.getRuleDocument()), botKeyboardFactory.rulesKeyboard());
    }

    public void sendRuleFile(Long chatId) {
        var ruleDocument = catalogClient.getRuleDocument();
        botMessageService.sendDocument(
                chatId,
                ruleDocument == null || ruleDocument.fileName() == null ? "river-park-rules.pdf" : ruleDocument.fileName(),
                catalogClient.downloadRuleDocument()
        );
    }

    public void openPhotoGallery(Long chatId, Long roomId, int roomPageNumber, Integer existingMessageId, boolean photoMessage) {
        openPhotoGallery(chatId, roomId, roomPageNumber, existingMessageId, photoMessage, false);
    }

    public void openPhotoGallery(Long chatId, Long roomId, int roomPageNumber, Integer existingMessageId, boolean photoMessage, boolean filtered) {
        RoomCategoryResponseDto room = catalogClient.getRoomById(roomId);
        List<RoomCategoryPhotoResponseDto> photos = conversationStore.getOrLoadPhotos(roomId, key -> catalogClient.getRoomPhotos(roomId, 0, botFlowProperties.pagination().photoPageSize()));

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
            showPhotoPage(chatId, existingMessageId, roomId, 0, roomPageNumber, filtered);
            return;
        }

        if (existingMessageId != null) {
            botMessageService.deleteMessage(chatId, existingMessageId);
        }

        String caption = botTextFactory.buildPhotosCaption(room, 0, photos.size());
        botMessageService.sendPhoto(chatId, photos.get(0).url(), caption, botKeyboardFactory.photoGallery(roomId, 0, photos.size(), roomPageNumber, filtered));
    }

    public void showPhotoPage(Long chatId, Integer messageId, Long roomId, int photoIndex, int roomPageNumber) {
        showPhotoPage(chatId, messageId, roomId, photoIndex, roomPageNumber, false);
    }

    public void showPhotoPage(Long chatId, Integer messageId, Long roomId, int photoIndex, int roomPageNumber, boolean filtered) {
        List<RoomCategoryPhotoResponseDto> photos = conversationStore.getOrLoadPhotos(roomId, key -> catalogClient.getRoomPhotos(roomId, 0, botFlowProperties.pagination().photoPageSize()));
        if (photos.isEmpty() || photoIndex < 0 || photoIndex >= photos.size()) {
            return;
        }
        RoomCategoryPhotoResponseDto photo = photos.get(photoIndex);
        String caption = botTextFactory.buildPhotosCaption(catalogClient.getRoomById(roomId), photoIndex, photos.size());
        if (!botMessageService.editPhoto(chatId, messageId, photo.url(), caption, botKeyboardFactory.photoGallery(roomId, photoIndex, photos.size(), roomPageNumber, filtered))) {
            botMessageService.deleteMessage(chatId, messageId);
            botMessageService.sendPhoto(chatId, photo.url(), caption, botKeyboardFactory.photoGallery(roomId, photoIndex, photos.size(), roomPageNumber, filtered));
        }
    }

    public RoomCategoryResponseDto resolveRoomForDisplay(Long chatId, Long roomId) {
        RoomCategoryResponseDto room = conversationStore.getAvailableRoom(chatId, roomId);
        if (room != null) {
            return room;
        }
        return catalogClient.getRoomById(roomId);
    }

    private AvailableRoomSearchDraft displayAvailabilityDraft(Long chatId) {
        AvailableRoomSearchDraft draft = conversationStore.getLastAvailabilityDraft(chatId);
        if (draft != null && isValidAvailabilityPeriod(draft.checkInDate(), draft.checkOutDate())) {
            return draft;
        }

        LocalDate today = LocalDate.now(NOVOSIBIRSK_ZONE);
        return AvailableRoomSearchDraft.builder()
                .checkInDate(today)
                .checkOutDate(today.plusDays(1))
                .build();
    }

    private boolean isValidAvailabilityPeriod(LocalDate checkInDate, LocalDate checkOutDate) {
        return checkInDate != null
                && checkOutDate != null
                && !checkInDate.isBefore(LocalDate.now(NOVOSIBIRSK_ZONE))
                && checkOutDate.isAfter(checkInDate);
    }

    private boolean isFilterEmpty(RoomCategorySearchRequestDto filter) {
        return filter == null
                || (filter.guests() == null
                && filter.roomType() == null
                && filter.priceFrom() == null
                && filter.priceTo() == null
                && filter.minArea() == null);
    }

    private boolean hasCatalogFilter(AvailableRoomSearchDraft draft) {
        return draft != null
                && (draft.guests() != null
                || draft.roomType() != null
                || draft.priceFrom() != null
                || draft.priceTo() != null
                || draft.minArea() != null);
    }

    private RoomCategorySearchRequestDto toCatalogFilter(AvailableRoomSearchDraft draft) {
        return RoomCategorySearchRequestDto.builder()
                .guests(draft.guests())
                .roomType(draft.roomType())
                .priceFrom(draft.priceFrom())
                .priceTo(draft.priceTo())
                .minArea(draft.minArea())
                .build();
    }

    private void cacheAvailableRooms(Long chatId, PageResponse<RoomCategoryResponseDto> page) {
        if (page == null || page.content() == null || page.content().isEmpty()) {
            return;
        }
        for (RoomCategoryResponseDto room : page.content()) {
            if (room != null && room.id() != null) {
                conversationStore.putAvailableRoom(chatId, room);
            }
        }
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
}
