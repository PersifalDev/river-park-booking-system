package ru.haritonenko.paymentservice.domain.db.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.haritonenko.paymentservice.domain.db.entity.PaymentEntity;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentEntityRepository extends JpaRepository<PaymentEntity, UUID> {
    Optional<PaymentEntity> findByBookingId(UUID bookingId);
    Page<PaymentEntity> findAllByUserId(Long userId, Pageable pageable);
}
