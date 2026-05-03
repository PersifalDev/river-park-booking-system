package ru.haritonenko.bookingservice.domain.db.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
}
