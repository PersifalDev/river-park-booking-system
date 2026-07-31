package ru.haritonenko.bookingservice.kafka.outbox.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.haritonenko.bookingservice.kafka.outbox.db.BookingOutboxEntity;
import ru.haritonenko.bookingservice.kafka.outbox.status.OutboxStatus;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface BookingOutboxRepository extends JpaRepository<BookingOutboxEntity, UUID> {

    @Query(value = """
            select *
            from booking_outbox
            where status in ('NEW', 'PROCESSING')
              and next_attempt_at <= :now
            order by created_at asc
            limit :batchSize
            for update skip locked
            """, nativeQuery = true)
    List<BookingOutboxEntity> findReadyForUpdate(
            @Param("now") OffsetDateTime now,
            @Param("batchSize") int batchSize
    );

    long countByStatus(OutboxStatus status);
}
