package ru.haritonenko.telegrambot.bot.service.handler;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import ru.haritonenko.telegrambot.bot.service.storage.BotConversationStore;
import ru.haritonenko.telegrambot.bot.state.AvailableRoomSearchDraft;
import ru.haritonenko.telegrambot.bot.state.ChatStateService;
import ru.haritonenko.telegrambot.bot.state.ChatStateType;
import ru.haritonenko.telegrambot.bot.util.BotKeyboardFactory;
import ru.haritonenko.telegrambot.bot.util.BotTextFactory;
import ru.haritonenko.telegrambot.config.BotFlowBookingProperties;
import ru.haritonenko.telegrambot.config.BotFlowFilterProperties;
import ru.haritonenko.telegrambot.config.BotFlowPaginationProperties;
import ru.haritonenko.telegrambot.config.BotFlowProperties;
import ru.haritonenko.telegrambot.service.BotMessageService;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomFilterFlowHandlerTest {

    private static final Long CHAT_ID = 100L;
    private static final Integer MESSAGE_ID = 10;

    @Mock
    private BotMessageService botMessageService;
    @Mock
    private BotKeyboardFactory botKeyboardFactory;
    @Mock
    private BotTextFactory botTextFactory;
    @Mock
    private CatalogFlowHandler catalogFlowHandler;
    @Mock
    private InlineKeyboardMarkup inlineKeyboard;
    @Mock
    private ReplyKeyboardMarkup replyKeyboard;

    private ChatStateService chatStateService;
    private BotConversationStore conversationStore;
    private RoomFilterFlowHandler handler;

    @BeforeEach
    void setUp() {
        chatStateService = new ChatStateService();
        conversationStore = new BotConversationStore();
        handler = new RoomFilterFlowHandler(
                botMessageService,
                botKeyboardFactory,
                botTextFactory,
                chatStateService,
                botFlowProperties(),
                catalogFlowHandler,
                conversationStore
        );
    }

    @Test
    void shouldRequestGuestsFromFilterButton() {
        when(botTextFactory.buildAskBookingAdultsMessage(null, null, 4, 6)).thenReturn("adults");
        when(botKeyboardFactory.mainMenu()).thenReturn(replyKeyboard);

        handler.requestGuests(CHAT_ID, MESSAGE_ID, false);

        assertEquals(ChatStateType.WAITING_FILTER_ADULTS, chatStateService.get(CHAT_ID).type());
        verify(botMessageService).deleteMessage(CHAT_ID, MESSAGE_ID);
        verify(botMessageService).sendText(CHAT_ID, "adults", replyKeyboard);
    }

    @Test
    void shouldReturnToFilterMenuAfterGuestCompositionInputWithoutSearching() {
        when(botTextFactory.buildFilterMenuMessage(any(AvailableRoomSearchDraft.class), anyInt(), anyInt(), anyInt()))
                .thenReturn("filter menu");
        when(botKeyboardFactory.roomFilterMenu(any(AvailableRoomSearchDraft.class))).thenReturn(inlineKeyboard);
        when(botTextFactory.buildAskBookingChildrenMessage(2, 3, 6)).thenReturn("children");
        when(botKeyboardFactory.mainMenu()).thenReturn(replyKeyboard);

        handler.startRoomFilter(CHAT_ID);
        clearInvocations(botMessageService);
        handler.requestGuests(CHAT_ID, MESSAGE_ID, false);
        handler.handleAdults(CHAT_ID, "2");
        assertEquals(ChatStateType.WAITING_FILTER_CHILDREN, chatStateService.get(CHAT_ID).type());

        handler.handleChildren(CHAT_ID, "1");

        AvailableRoomSearchDraft draft = chatStateService.get(CHAT_ID).availableRoomSearchDraft();
        assertEquals(3, draft.guests());
        assertEquals(2, draft.adultCount());
        assertEquals(1, draft.childrenCount());
        assertEquals(ChatStateType.IDLE, chatStateService.get(CHAT_ID).type());
        verify(catalogFlowHandler, never()).sendRoomsPage(eq(CHAT_ID), anyInt(), any(), any(), eq(true));
        verify(botMessageService).sendText(CHAT_ID, "filter menu", inlineKeyboard);
    }

    @Test
    void shouldSearchOnlyAfterSearchButton() {
        handler.search(CHAT_ID, MESSAGE_ID, false);

        assertEquals(ChatStateType.IDLE, chatStateService.get(CHAT_ID).type());
        verify(botMessageService).deleteMessage(CHAT_ID, MESSAGE_ID);
        verify(catalogFlowHandler).sendRoomsPage(CHAT_ID, 0, null, null, true);
    }

    @Test
    void shouldOpenPriceBoundaryMenuFromPriceButton() {
        when(botTextFactory.buildAskPriceBoundaryButtonMessage()).thenReturn("price boundary");
        when(botKeyboardFactory.priceFilterMenu()).thenReturn(inlineKeyboard);

        handler.requestPrice(CHAT_ID, MESSAGE_ID, false);

        assertEquals(ChatStateType.IDLE, chatStateService.get(CHAT_ID).type());
        verify(botMessageService).editText(CHAT_ID, MESSAGE_ID, "price boundary", inlineKeyboard);
    }

    @Test
    void shouldReturnToFilterMenuAfterMinPriceWithoutMaxPrice() {
        when(botTextFactory.buildFilterMenuMessage(any(AvailableRoomSearchDraft.class), anyInt(), anyInt(), anyInt()))
                .thenReturn("filter menu");
        when(botKeyboardFactory.roomFilterMenu(any(AvailableRoomSearchDraft.class))).thenReturn(inlineKeyboard);

        handler.handlePriceFrom(CHAT_ID, "7900");

        AvailableRoomSearchDraft draft = chatStateService.get(CHAT_ID).availableRoomSearchDraft();
        assertEquals(0, BigDecimal.valueOf(7900).compareTo(draft.priceFrom()));
        assertNull(draft.priceTo());
        assertEquals(ChatStateType.IDLE, chatStateService.get(CHAT_ID).type());
        verify(botMessageService).sendText(CHAT_ID, "filter menu", inlineKeyboard);
    }

    @Test
    void shouldReturnToFilterMenuAfterMaxPriceWithoutMinPrice() {
        when(botTextFactory.buildFilterMenuMessage(any(AvailableRoomSearchDraft.class), anyInt(), anyInt(), anyInt()))
                .thenReturn("filter menu");
        when(botKeyboardFactory.roomFilterMenu(any(AvailableRoomSearchDraft.class))).thenReturn(inlineKeyboard);

        handler.handlePriceTo(CHAT_ID, "10000");

        AvailableRoomSearchDraft draft = chatStateService.get(CHAT_ID).availableRoomSearchDraft();
        assertNull(draft.priceFrom());
        assertEquals(0, BigDecimal.valueOf(10000).compareTo(draft.priceTo()));
        assertEquals(ChatStateType.IDLE, chatStateService.get(CHAT_ID).type());
        verify(botMessageService).sendText(CHAT_ID, "filter menu", inlineKeyboard);
    }

    @Test
    void shouldClearGuestsAndReturnToMenuWhenSkipIsTyped() {
        when(botTextFactory.buildFilterMenuMessage(any(AvailableRoomSearchDraft.class), anyInt(), anyInt(), anyInt()))
                .thenReturn("filter menu");
        when(botKeyboardFactory.roomFilterMenu(any(AvailableRoomSearchDraft.class))).thenReturn(inlineKeyboard);

        chatStateService.updateAvailableRoomSearchDraft(CHAT_ID, AvailableRoomSearchDraft.builder()
                .guests(3)
                .adultCount(2)
                .childrenCount(1)
                .checkInDate(LocalDate.now().plusDays(1))
                .checkOutDate(LocalDate.now().plusDays(2))
                .build());
        chatStateService.setType(CHAT_ID, ChatStateType.WAITING_FILTER_ADULTS);

        handler.handleAdults(CHAT_ID, "-");

        AvailableRoomSearchDraft draft = chatStateService.get(CHAT_ID).availableRoomSearchDraft();
        assertNull(draft.guests());
        assertNull(draft.adultCount());
        assertNull(draft.childrenCount());
        assertEquals(ChatStateType.IDLE, chatStateService.get(CHAT_ID).type());
        verify(botMessageService).sendText(CHAT_ID, "filter menu", inlineKeyboard);
    }

    private BotFlowProperties botFlowProperties() {
        return new BotFlowProperties(
                new BotFlowBookingProperties(6, 4, 3),
                new BotFlowFilterProperties(
                        6,
                        BigDecimal.valueOf(5000),
                        BigDecimal.valueOf(20000),
                        BigDecimal.valueOf(10),
                        BigDecimal.valueOf(60)
                ),
                new BotFlowPaginationProperties(5, 5, 5, 5, 5)
        );
    }
}
