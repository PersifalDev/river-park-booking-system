package ru.haritonenko.telegrambot.bot.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import ru.haritonenko.telegrambot.bot.service.handler.BookingActionFlowHandler;
import ru.haritonenko.telegrambot.bot.service.handler.BookingCreationFlowHandler;
import ru.haritonenko.telegrambot.bot.service.handler.BookingHistoryFlowHandler;
import ru.haritonenko.telegrambot.bot.service.handler.CatalogFlowHandler;
import ru.haritonenko.telegrambot.bot.service.handler.NotificationFlowHandler;
import ru.haritonenko.telegrambot.bot.service.handler.RoomFilterFlowHandler;
import ru.haritonenko.telegrambot.bot.service.storage.BotConversationStore;
import ru.haritonenko.telegrambot.bot.state.ChatStateService;
import ru.haritonenko.telegrambot.bot.util.BotKeyboardFactory;
import ru.haritonenko.telegrambot.bot.util.BotTextFactory;
import ru.haritonenko.telegrambot.client.BookingClient;
import ru.haritonenko.telegrambot.client.CatalogClient;
import ru.haritonenko.telegrambot.config.BookingClientProperties;
import ru.haritonenko.telegrambot.config.BotFlowProperties;
import ru.haritonenko.telegrambot.config.BotProperties;
import ru.haritonenko.telegrambot.config.CatalogClientProperties;
import ru.haritonenko.telegrambot.config.NotificationClientProperties;
import ru.haritonenko.telegrambot.config.PaymentClientProperties;
import ru.haritonenko.telegrambot.config.UserClientProperties;
import ru.haritonenko.telegrambot.service.BotMessageService;
import ru.haritonenko.telegrambot.service.auth.BotAuthService;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BotUpdateServiceFilterCallbackTest {

    @Mock
    private CatalogClient catalogClient;
    @Mock
    private BookingClient bookingClient;
    @Mock
    private BotAuthService botAuthService;
    @Mock
    private BotMessageService botMessageService;
    @Mock
    private BotKeyboardFactory botKeyboardFactory;
    @Mock
    private BotTextFactory botTextFactory;
    @Mock
    private ChatStateService chatStateService;
    @Mock
    private BotProperties botProperties;
    @Mock
    private BotFlowProperties botFlowProperties;
    @Mock
    private BookingClientProperties bookingClientProperties;
    @Mock
    private CatalogClientProperties catalogClientProperties;
    @Mock
    private PaymentClientProperties paymentClientProperties;
    @Mock
    private NotificationClientProperties notificationClientProperties;
    @Mock
    private UserClientProperties userClientProperties;
    @Mock
    private NotificationFlowHandler notificationFlowHandler;
    @Mock
    private BookingHistoryFlowHandler bookingHistoryFlowHandler;
    @Mock
    private BookingActionFlowHandler bookingActionFlowHandler;
    @Mock
    private CatalogFlowHandler catalogFlowHandler;
    @Mock
    private RoomFilterFlowHandler roomFilterFlowHandler;
    @Mock
    private BookingCreationFlowHandler bookingCreationFlowHandler;
    @Mock
    private BotConversationStore conversationStore;

    private BotUpdateService service;

    @BeforeEach
    void setUp() {
        service = new BotUpdateService(
                catalogClient,
                bookingClient,
                botAuthService,
                botMessageService,
                botKeyboardFactory,
                botTextFactory,
                chatStateService,
                botProperties,
                botFlowProperties,
                bookingClientProperties,
                catalogClientProperties,
                paymentClientProperties,
                notificationClientProperties,
                userClientProperties,
                notificationFlowHandler,
                bookingHistoryFlowHandler,
                bookingActionFlowHandler,
                catalogFlowHandler,
                roomFilterFlowHandler,
                bookingCreationFlowHandler,
                conversationStore
        );
        service.initActionStrategies();
    }

    @Test
    void shouldRouteFilterGuestsCallbackToFilterHandler() {
        Update update = callbackUpdate("filter:guests");

        service.handle(update);

        verify(roomFilterFlowHandler).requestGuests(100L, 10, false);
    }

    @Test
    void shouldRouteFilterMaxPriceCallbackToFilterHandler() {
        Update update = callbackUpdate("filter:price:to");

        service.handle(update);

        verify(roomFilterFlowHandler).requestPriceTo(100L, 10, false);
    }

    private Update callbackUpdate(String data) {
        Message message = mock(Message.class);
        when(message.getChatId()).thenReturn(100L);
        when(message.getMessageId()).thenReturn(10);

        CallbackQuery callbackQuery = new CallbackQuery();
        callbackQuery.setId("callback-id");
        callbackQuery.setData(data);
        callbackQuery.setMessage(message);

        Update update = new Update();
        update.setCallbackQuery(callbackQuery);
        return update;
    }
}
