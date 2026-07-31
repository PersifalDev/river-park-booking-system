package ru.haritonenko.paymentservice.inbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.UUID;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, Long> {

    @Modifying
    @Query(value = """
            insert into processed_event(event_id, consumer_name, processed_at)
            values (:eventId, :consumerName, :processedAt)
            on conflict (event_id, consumer_name) do nothing
            """, nativeQuery = true)
    int tryInsert(
            @Param("eventId") UUID eventId,
            @Param("consumerName") String consumerName,
            @Param("processedAt") OffsetDateTime processedAt
    );
}
