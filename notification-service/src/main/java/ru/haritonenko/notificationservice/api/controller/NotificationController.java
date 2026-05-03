package ru.haritonenko.notificationservice.api.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.haritonenko.commonlibs.security.authorization.user.AuthUser;
import ru.haritonenko.notificationservice.api.dto.filter.NotificationPageFilter;
import ru.haritonenko.notificationservice.api.dto.NotificationResponseDto;
import ru.haritonenko.notificationservice.domain.mapper.NotificationMapper;
import ru.haritonenko.notificationservice.domain.service.NotificationService;
import ru.haritonenko.notificationservice.security.service.AuthenticationService;

import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;
    private final AuthenticationService authenticationService;

    @GetMapping
    public ResponseEntity<Page<NotificationResponseDto>> getAllNotifications(@ModelAttribute NotificationPageFilter pageFilter) {
        Long authUserId = getAuthenticatedUser().id();
        log.info("Request for getting all notifications for userId={}", authUserId);
        return ResponseEntity.ok(notificationService.getAllNotificationsByUserId(authUserId, pageFilter).map(notificationMapper::toDto));
    }

    @GetMapping("/unread")
    public ResponseEntity<Page<NotificationResponseDto>> getUnreadNotifications(@ModelAttribute NotificationPageFilter pageFilter) {
        Long authUserId = getAuthenticatedUser().id();
        log.info("Request for getting unread notifications for userId={}", authUserId);
        return ResponseEntity.ok(notificationService.getUnreadNotificationsByUserId(authUserId, pageFilter).map(notificationMapper::toDto));
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponseDto> markAsRead(@PathVariable UUID notificationId) {
        Long authUserId = getAuthenticatedUser().id();
        log.info("Request for marking notification as read: notificationId={}, userId={}", notificationId, authUserId);
        return ResponseEntity.ok(notificationMapper.toDto(notificationService.markAsRead(notificationId, authUserId)));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead() {
        Long authUserId = getAuthenticatedUser().id();
        log.info("Request for marking all notifications as read for userId={}", authUserId);
        notificationService.markAllAsRead(authUserId);
        return ResponseEntity.noContent().build();
    }

    private AuthUser getAuthenticatedUser() {
        AuthUser authUser = authenticationService.getCurrentAuthenticatedUser();
        log.info("Authenticated notification-service user resolved: userId={}", authUser.id());
        return authUser;
    }
}
