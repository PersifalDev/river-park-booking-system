package ru.haritonenko.notificationservice.api.controller.all_context;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.haritonenko.commonlibs.dto.kafka.event.type.NotificationEventType;
import ru.haritonenko.commonlibs.notification.NotificationStatus;
import ru.haritonenko.commonlibs.security.authorization.user.AuthUser;
import ru.haritonenko.notificationservice.api.dto.filter.NotificationPageFilter;
import ru.haritonenko.notificationservice.domain.db.entity.NotificationEntity;
import ru.haritonenko.notificationservice.domain.kafka.consumer.booking.listener.NotificationKafkaBookingEventListener;
import ru.haritonenko.notificationservice.domain.kafka.consumer.direct.listener.NotificationKafkaDirectEventListener;
import ru.haritonenko.notificationservice.domain.kafka.consumer.payment.listener.NotificationKafkaPaymentEventListener;
import ru.haritonenko.notificationservice.domain.mapper.NotificationMapper;
import ru.haritonenko.notificationservice.domain.service.NotificationService;
import ru.haritonenko.notificationservice.security.jwt.manager.JwtTokenManager;
import ru.haritonenko.notificationservice.security.service.AuthenticationService;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:notification-controller-context;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.kafka.listener.auto-startup=false",
        "spring.liquibase.enabled=false"
})
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerAllContextTest {

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

    @MockitoBean
    private NotificationKafkaBookingEventListener bookingEventListener;

    @MockitoBean
    private NotificationKafkaPaymentEventListener paymentEventListener;

    @MockitoBean
    private NotificationKafkaDirectEventListener directEventListener;

    @Test
    void shouldLoadApplicationContextAndGetUnreadNotifications() throws Exception {
        NotificationEntity notification = notification();
        when(authenticationService.getCurrentAuthenticatedUser())
                .thenReturn(AuthUser.builder().id(USER_ID).login("watson").role("USER").build());
        when(notificationService.getUnreadNotificationsByUserId(eq(USER_ID), any(NotificationPageFilter.class)))
                .thenReturn(new PageImpl<>(List.of(notification)));
        when(notificationMapper.toDto(any(NotificationEntity.class))).thenCallRealMethod();

        mockMvc.perform(get("/notifications/unread"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].read").value(false));
    }

    private NotificationEntity notification() {
        return NotificationEntity.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .bookingId(UUID.randomUUID())
                .title("Бронь обновлена")
                .message("Статус брони изменился")
                .type(NotificationEventType.BOOKING_CONFIRMED)
                .status(NotificationStatus.NEW)
                .read(false)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
    }
}
