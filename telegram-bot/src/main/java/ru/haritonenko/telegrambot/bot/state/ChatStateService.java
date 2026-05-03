package ru.haritonenko.telegrambot.bot.state;

import org.springframework.stereotype.Service;
import ru.haritonenko.commonlibs.dto.category.RoomCategorySearchRequestDto;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ChatStateService {

    private final Map<Long, ChatState> stateByChatId = new ConcurrentHashMap<>();

    public ChatState get(Long chatId) {
        return stateByChatId.getOrDefault(chatId, ChatState.idle());
    }

    public void setType(Long chatId, ChatStateType type) {
        ChatState current = get(chatId);
        stateByChatId.put(chatId, current.toBuilder().type(type).build());
    }

    public void updateFilter(Long chatId, RoomCategorySearchRequestDto filter) {
        ChatState current = get(chatId);
        stateByChatId.put(chatId, current.toBuilder().filter(filter).build());
    }

    public void updateBookingDraft(Long chatId, BookingDraft bookingDraft) {
        ChatState current = get(chatId);
        stateByChatId.put(chatId, current.toBuilder().bookingDraft(bookingDraft).build());
    }

    public void updateAvailableRoomSearchDraft(Long chatId, AvailableRoomSearchDraft draft) {
        ChatState current = get(chatId);
        stateByChatId.put(chatId, current.toBuilder().availableRoomSearchDraft(draft).build());
    }

    public void reset(Long chatId) {
        stateByChatId.put(chatId, ChatState.idle());
    }
}
