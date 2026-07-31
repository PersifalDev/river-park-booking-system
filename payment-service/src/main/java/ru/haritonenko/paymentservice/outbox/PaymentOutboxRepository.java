package ru.haritonenko.paymentservice.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface PaymentOutboxRepository extends JpaRepository<PaymentOutboxEntity, UUID> {

    long countByStatus(PaymentOutboxStatus status);

    @Query(value = """
            select *
            from payment_outbox
            where status in ('NEW', 'PROCESSING')
              and next_attempt_at <= :now
            order by created_at asc
            limit :batchSize
            for update skip locked
            """, nativeQuery = true)
    List<PaymentOutboxEntity> findReadyForUpdate(
            @Param("now") OffsetDateTime now,
            @Param("batchSize") int batchSize
    );
}
