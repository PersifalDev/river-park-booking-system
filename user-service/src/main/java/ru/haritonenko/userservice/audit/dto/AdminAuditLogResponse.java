package ru.haritonenko.userservice.audit.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AdminAuditLogResponse(
        UUID id,
        Long actorUserId,
        String actorLogin,
        String actorRole,
        String action,
        String targetType,
        String targetId,
        String outcome,
        String requestId,
        String ipAddress,
        String userAgent,
        String details,
        OffsetDateTime createdAt
) {
}
