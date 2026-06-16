package ru.haritonenko.notificationservice.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import ru.haritonenko.commonlibs.utils.pages.PageResponse;
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
@Tag(name = "Notifications", description = "Уведомления пользователя")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationMapper notificationMapper;
    private final AuthenticationService authenticationService;

    @GetMapping
    @Operation(summary = "Получить все уведомления")
    @ApiResponse(responseCode = "200", description = "Страница уведомлений")
    public ResponseEntity<PageResponse<NotificationResponseDto>> getAllNotifications(@ModelAttribute NotificationPageFilter pageFilter) {
        Long authUserId = getAuthenticatedUser().id();
        log.info("Request for getting all notifications for userId={}", authUserId);
        return ResponseEntity.ok(toPageResponse(notificationService.getAllNotificationsByUserId(authUserId, pageFilter).map(notificationMapper::toDto)));
    }

    @GetMapping("/unread")
    @Operation(summary = "Получить непрочитанные уведомления")
    @ApiResponse(responseCode = "200", description = "Страница непрочитанных уведомлений")
    public ResponseEntity<PageResponse<NotificationResponseDto>> getUnreadNotifications(@ModelAttribute NotificationPageFilter pageFilter) {
        Long authUserId = getAuthenticatedUser().id();
        log.info("Request for getting unread notifications for userId={}", authUserId);
        return ResponseEntity.ok(toPageResponse(notificationService.getUnreadNotificationsByUserId(authUserId, pageFilter).map(notificationMapper::toDto)));
    }

    @PatchMapping("/{notificationId}/read")
    @Operation(summary = "Отметить уведомление прочитанным")
    @ApiResponse(responseCode = "200", description = "Уведомление обновлено")
    public ResponseEntity<NotificationResponseDto> markAsRead(
            @Parameter(description = "UUID уведомления") @PathVariable UUID notificationId
    ) {
        Long authUserId = getAuthenticatedUser().id();
        log.info("Request for marking notification as read: notificationId={}, userId={}", notificationId, authUserId);
        return ResponseEntity.ok(notificationMapper.toDto(notificationService.markAsRead(notificationId, authUserId)));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Отметить все уведомления прочитанными")
    @ApiResponse(responseCode = "204", description = "Все уведомления отмечены прочитанными")
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

    private <T> PageResponse<T> toPageResponse(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getTotalPages(),
                page.getTotalElements(),
                page.getSize(),
                page.getNumber()
        );
    }
}
