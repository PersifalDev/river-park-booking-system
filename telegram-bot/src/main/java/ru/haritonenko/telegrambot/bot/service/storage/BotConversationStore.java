package ru.haritonenko.telegrambot.bot.service.storage;

import org.springframework.stereotype.Component;
import ru.haritonenko.commonlibs.dto.category.RoomCategoryResponseDto;
import ru.haritonenko.commonlibs.dto.photo.RoomCategoryPhotoResponseDto;
import ru.haritonenko.telegrambot.bot.state.AvailableRoomSearchDraft;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

@Component
public class BotConversationStore {

    private final Map<Long, List<RoomCategoryPhotoResponseDto>> photoCache = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, RoomCategoryResponseDto>> availableRoomCache = new ConcurrentHashMap<>();
    private final Map<Long, AvailableRoomSearchDraft> lastAvailabilityDraftByChatId = new ConcurrentHashMap<>();

    public List<RoomCategoryPhotoResponseDto> getOrLoadPhotos(
            Long roomId,
            Function<Long, List<RoomCategoryPhotoResponseDto>> loader
    ) {
        return photoCache.computeIfAbsent(roomId, loader);
    }

    public RoomCategoryResponseDto getAvailableRoom(Long chatId, Long roomId) {
        Map<Long, RoomCategoryResponseDto> rooms = availableRoomCache.get(chatId);
        return rooms == null ? null : rooms.get(roomId);
    }

    public boolean hasAvailableRoom(Long chatId, Long roomId) {
        Map<Long, RoomCategoryResponseDto> rooms = availableRoomCache.get(chatId);
        return rooms != null && rooms.containsKey(roomId);
    }

    public void putAvailableRoom(Long chatId, RoomCategoryResponseDto room) {
        availableRoomCache.computeIfAbsent(chatId, ignored -> new ConcurrentHashMap<>()).put(room.id(), room);
    }

    public void removeAvailableRooms(Long chatId) {
        availableRoomCache.remove(chatId);
    }

    public void putLastAvailabilityDraft(Long chatId, AvailableRoomSearchDraft draft) {
        lastAvailabilityDraftByChatId.put(chatId, draft);
    }

    public void removeLastAvailabilityDraft(Long chatId) {
        lastAvailabilityDraftByChatId.remove(chatId);
    }

    public AvailableRoomSearchDraft getLastAvailabilityDraft(Long chatId) {
        return lastAvailabilityDraftByChatId.get(chatId);
    }
}
