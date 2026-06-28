package ru.haritonenko.userservice.audit.service;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.haritonenko.commonlibs.observability.RequestIdFilter;
import ru.haritonenko.commonlibs.security.authorization.user.AuthUser;
import ru.haritonenko.userservice.audit.db.entity.AdminAuditLogEntity;
import ru.haritonenko.userservice.audit.db.repository.AdminAuditLogRepository;
import ru.haritonenko.userservice.audit.dto.AdminAuditLogResponse;

@Service
@RequiredArgsConstructor
public class AdminAuditLogService {

    private final AdminAuditLogRepository repository;

    @Transactional(readOnly = true)
    public Page<AdminAuditLogResponse> getAuditLog(Pageable pageable) {
        return repository.findAll(pageable).map(this::toResponse);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String action, String targetType, String targetId, String outcome, String details, HttpServletRequest request) {
        AuthUser actor = currentActor();
        repository.save(AdminAuditLogEntity.builder()
                .actorUserId(actor == null ? null : actor.id())
                .actorLogin(actor == null ? null : actor.login())
                .actorRole(actor == null ? null : actor.role())
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .outcome(outcome)
                .requestId(MDC.get(RequestIdFilter.REQUEST_ID_MDC_KEY))
                .ipAddress(clientIp(request))
                .userAgent(limit(request.getHeader("User-Agent"), 256))
                .details(limit(details, 1024))
                .build());
    }

    private AuthUser currentActor() {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthUser authUser)) {
            return null;
        }
        return authUser;
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return limit(forwardedFor.split(",")[0].trim(), 64);
        }
        return limit(request.getRemoteAddr(), 64);
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.length() <= maxLength ? trimmed : trimmed.substring(0, maxLength);
    }

    private AdminAuditLogResponse toResponse(AdminAuditLogEntity entity) {
        return new AdminAuditLogResponse(
                entity.getId(),
                entity.getActorUserId(),
                entity.getActorLogin(),
                entity.getActorRole(),
                entity.getAction(),
                entity.getTargetType(),
                entity.getTargetId(),
                entity.getOutcome(),
                entity.getRequestId(),
                entity.getIpAddress(),
                entity.getUserAgent(),
                entity.getDetails(),
                entity.getCreatedAt()
        );
    }
}
