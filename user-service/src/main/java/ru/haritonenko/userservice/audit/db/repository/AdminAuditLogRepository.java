package ru.haritonenko.userservice.audit.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.haritonenko.userservice.audit.db.entity.AdminAuditLogEntity;

import java.util.UUID;

public interface AdminAuditLogRepository extends JpaRepository<AdminAuditLogEntity, UUID> {
}
