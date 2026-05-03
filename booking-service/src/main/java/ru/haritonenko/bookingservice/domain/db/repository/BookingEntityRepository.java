package ru.haritonenko.bookingservice.domain.db.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.haritonenko.bookingservice.domain.db.entity.BookingEntity;
import ru.haritonenko.bookingservice.domain.status.BookingStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingEntityRepository extends JpaRepository<BookingEntity, UUID>, JpaSpecificationExecutor<BookingEntity> {

    Optional<BookingEntity> findByIdAndUserId(UUID id, Long userId);

    Page<BookingEntity> findAllByUserIdAndStatusIn(Long userId, Collection<BookingStatus> statuses, Pageable pageable);

    @Query("""
            SELECT b from BookingEntity b
            WHERE b.status = :status
              AND b.holdExpiresAt IS NOT NULL
              AND b.holdExpiresAt < :now
            """)
    List<BookingEntity> findExpiredHolds(@Param("status") BookingStatus status, @Param("now") OffsetDateTime now);

    @Query("""
            SELECT b from BookingEntity b
            WHERE b.status = :status
              AND b.holdExpiresAt IS NOT NULL
              AND b.holdExpiresAt < :now
            """)
    List<BookingEntity> findExpiredCreatedDrafts(@Param("status") BookingStatus status, @Param("now") OffsetDateTime now);

    @Query("""
            SELECT b from BookingEntity b
            WHERE b.status = :status
              AND b.holdExpiresAt IS NOT NULL
              AND b.holdReminderSentAt IS NULL
              AND b.holdExpiresAt BETWEEN :from AND :to
            ORDER BY b.holdExpiresAt ASC
            """)
    List<BookingEntity> findHoldBookingsForReminder(
            @Param("status") BookingStatus status,
            @Param("from") OffsetDateTime from,
            @Param("to") OffsetDateTime to
    );

    @Query("""
            SELECT b from BookingEntity b
            WHERE b.status = :status
              AND b.checkInReminderSentAt IS NULL
              AND b.checkInDate = :targetDate
            ORDER BY b.checkInDate ASC, b.createdAt ASC
            """)
    List<BookingEntity> findBookingsForCheckInReminder(
            @Param("status") BookingStatus status,
            @Param("targetDate") LocalDate targetDate
    );

    @Modifying
    @Query("""
            DELETE FROM BookingEntity b
            WHERE b.status in :statuses
              AND b.createdAt < :threshold
            """)
    int deleteInactiveBookingsCreatedBefore(
            @Param("statuses") Collection<BookingStatus> statuses,
            @Param("threshold") OffsetDateTime threshold
    );
}
