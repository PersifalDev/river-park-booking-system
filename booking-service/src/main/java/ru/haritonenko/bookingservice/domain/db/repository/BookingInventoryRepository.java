package ru.haritonenko.bookingservice.domain.db.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.haritonenko.bookingservice.domain.db.entity.BookingInventoryEntity;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingInventoryRepository extends JpaRepository<BookingInventoryEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT i from BookingInventoryEntity i
            WHERE i.roomCategoryId = :roomCategoryId
              AND i.bookingDate >= :fromDate
              AND i.bookingDate < :toDate
            ORDER BY  i.bookingDate ASC
            """)
    List<BookingInventoryEntity> findForUpdateByRoomCategoryIdAndBookingDateBetween(
            @Param("roomCategoryId") Long roomCategoryId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    Optional<BookingInventoryEntity> findByRoomCategoryIdAndBookingDate(Long roomCategoryId, LocalDate bookingDate);

    @Modifying
    @Query(value = """
            INSERT INTO booking_inventory(room_category_id, booking_date, total_units, held_units, confirmed_units, created_at, updated_at)
            SELECT :roomCategoryId, day::date, :totalUnits, 0, 0, now(), now()
            FROM generate_series(CAST(:fromDate AS timestamp), CAST(:toDate AS timestamp) - interval '1 day', interval '1 day') AS day
            ON CONFLICT (room_category_id, booking_date) DO NOTHING
            """, nativeQuery = true)
    void insertMissingRows(
            @Param("roomCategoryId") Long roomCategoryId,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate,
            @Param("totalUnits") Integer totalUnits
    );
}
