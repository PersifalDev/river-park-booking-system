package ru.haritonenko.userservice.consent.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.haritonenko.userservice.consent.db.entity.UserConsentEntity;

import java.util.UUID;

public interface UserConsentRepository extends JpaRepository<UserConsentEntity, UUID> {
}
