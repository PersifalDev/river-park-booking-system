package ru.haritonenko.bookingservice.domain.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.haritonenko.bookingservice.domain.db.entity.BookingIdempotencyKeyEntity;

import java.time.OffsetDateTime;
import java.util.Optional;

@Repository
public interface BookingIdempotencyKeyRepository extends JpaRepository<BookingIdempotencyKeyEntity, Long> {

    Optional<BookingIdempotencyKeyEntity> findByUserIdAndIdempotencyKeyAndExpiresAtAfter(
            Long userId,
            String idempotencyKey,
            OffsetDateTime now
    );
}
