package ru.haritonenko.telegrambot.bot.state;

import lombok.Builder;
import ru.haritonenko.commonlibs.dto.category.RoomCategorySearchRequestDto;

@Builder(toBuilder = true)
public record ChatState(
        ChatStateType type,
        RoomCategorySearchRequestDto filter,
        BookingDraft bookingDraft,
        AvailableRoomSearchDraft availableRoomSearchDraft
) {

    public static ChatState idle() {
        return ChatState.builder()
                .type(ChatStateType.IDLE)
                .filter(RoomCategorySearchRequestDto.builder().build())
                .bookingDraft(BookingDraft.empty())
                .availableRoomSearchDraft(AvailableRoomSearchDraft.empty())
                .build();
    }
}
