package ru.haritonenko.bookingservice.domain.db.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;
import ru.haritonenko.bookingservice.domain.db.entity.PromoCodeEntity;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PromoCodeRepository extends JpaRepository<PromoCodeEntity, Long> {

    boolean existsByCode(String code);

    boolean existsBySourceBookingId(UUID sourceBookingId);

    Optional<PromoCodeEntity> findByCode(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PromoCodeEntity> findForUpdateByCodeAndUserIdAndUsedFalse(String code, Long userId);
}
