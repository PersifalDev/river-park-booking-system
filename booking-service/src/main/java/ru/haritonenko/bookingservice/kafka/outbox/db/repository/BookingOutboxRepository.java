package ru.haritonenko.bookingservice.kafka.outbox.db.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.haritonenko.bookingservice.kafka.outbox.db.BookingOutboxEntity;
import ru.haritonenko.bookingservice.kafka.outbox.status.OutboxStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface BookingOutboxRepository extends JpaRepository<BookingOutboxEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select e
            from BookingOutboxEntity e
            where e.status = :status
              and e.nextAttemptAt <= :now
            order by e.createdAt asc
            """)
    List<BookingOutboxEntity> findReadyForUpdate(
            @Param("status") OutboxStatus status,
            @Param("now") OffsetDateTime now,
            Pageable pageable
    );
}