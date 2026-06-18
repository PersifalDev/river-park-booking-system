package ru.haritonenko.notificationservice.api.controller.web_only;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.haritonenko.commonlibs.dto.kafka.event.type.NotificationEventType;
import ru.haritonenko.commonlibs.notification.NotificationStatus;
import ru.haritonenko.commonlibs.security.authorization.user.AuthUser;
import ru.haritonenko.notificationservice.api.controller.NotificationController;
import ru.haritonenko.notificationservice.api.dto.filter.NotificationPageFilter;
import ru.haritonenko.notificationservice.domain.db.entity.NotificationEntity;
import ru.haritonenko.notificationservice.domain.mapper.NotificationMapper;
import ru.haritonenko.notificationservice.domain.service.NotificationService;
import ru.haritonenko.notificationservice.security.jwt.manager.JwtTokenManager;
import ru.haritonenko.notificationservice.security.service.AuthenticationService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = NotificationController.class)
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerWebTest {

    private static final Long USER_ID = 10L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService notificationService;

    @MockitoBean
    private NotificationMapper notificationMapper;

    @MockitoBean
    private AuthenticationService authenticationService;

    @MockitoBean
    private JwtTokenManager jwtTokenManager;

    private NotificationEntity notification;

    @BeforeEach
    void setUp() {
        notification = notification(false);
        when(authenticationService.getCurrentAuthenticatedUser())
                .thenReturn(AuthUser.builder().id(USER_ID).login("watson").role("USER").build());
        when(notificationMapper.toDto(any(NotificationEntity.class))).thenCallRealMethod();
    }

    @Test
    void shouldGetAllNotifications() throws Exception {
        when(notificationService.getAllNotificationsByUserId(eq(USER_ID), any(NotificationPageFilter.class)))
                .thenReturn(new PageImpl<>(List.of(notification)));

        mockMvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("Бронь обновлена"));

        verify(notificationService).getAllNotificationsByUserId(eq(USER_ID), any(NotificationPageFilter.class));
    }

    @Test
    void shouldGetUnreadNotifications() throws Exception {
        when(notificationService.getUnreadNotificationsByUserId(eq(USER_ID), any(NotificationPageFilter.class)))
                .thenReturn(new PageImpl<>(List.of(notification)));

        mockMvc.perform(get("/notifications/unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].read").value(false));

        verify(notificationService).getUnreadNotificationsByUserId(eq(USER_ID), any(NotificationPageFilter.class));
    }

    @Test
    void shouldMarkNotificationAsRead() throws Exception {
        NotificationEntity read = notification(true);
        when(notificationService.markAsRead(notification.getId(), USER_ID)).thenReturn(read);

        mockMvc.perform(patch("/notifications/{notificationId}/read", notification.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.read").value(true))
                .andExpect(jsonPath("$.status").value("READ"));

        verify(notificationService).markAsRead(notification.getId(), USER_ID);
    }

    @Test
    void shouldMarkAllNotificationsAsRead() throws Exception {
        mockMvc.perform(patch("/notifications/read-all"))
                .andExpect(status().isNoContent());

        verify(notificationService).markAllAsRead(USER_ID);
    }

    private NotificationEntity notification(boolean read) {
        return NotificationEntity.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .bookingId(UUID.randomUUID())
                .title("Бронь обновлена")
                .message("Статус брони изменился")
                .type(NotificationEventType.BOOKING_CONFIRMED)
                .status(read ? NotificationStatus.READ : NotificationStatus.NEW)
                .read(read)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }
}
