package ru.haritonenko.bookingservice.tasks.domain.async.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.haritonenko.bookingservice.tasks.domain.async.db.entity.AsyncBookingTaskEntity;
import ru.haritonenko.bookingservice.tasks.domain.async.status.AsyncBookingTaskStatus;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface AsyncBookingTaskEntityRepository extends JpaRepository<AsyncBookingTaskEntity, Long> {

    @Query("""
            SELECT t from AsyncBookingTaskEntity t
            WHERE t.status in :statuses
              AND (t.nextAttemptAt IS NULL OR t.nextAttemptAt <= :now)
            ORDER BY t.createdAt ASC
            """)
    List<AsyncBookingTaskEntity> findAvailableTasks(
            @Param("statuses") List<AsyncBookingTaskStatus> statuses,
            @Param("now") OffsetDateTime now
    );

    @Query(value = """
            SELECT * FROM async_booking_task as t 
            where t.task_status = :newStatus
            or (t.task_status = :retryStatus and t.next_attempt_at <= :now)
            or (t.task_status = :processingStatus and t.next_attempt_at <= :now)
            order by t.id
            limit :batchSize
            for update skip locked
            """, nativeQuery = true)
    List<AsyncBookingTaskEntity> pickBatchForProcessing(
            @Param("newStatus") int newStatus,
            @Param("retryStatus") int retryStatus,
            @Param("processingStatus") int processingStatus,
            @Param("batchSize") int batchSize,
            @Param("now") OffsetDateTime now

    );
}
