package ru.haritonenko.bookingservice.domain.db.repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.haritonenko.bookingservice.domain.db.entity.BookingRoomEntity;
import ru.haritonenko.bookingservice.domain.room.RoomOperationalStatus;
import ru.haritonenko.bookingservice.domain.status.BookingStatus;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRoomRepository extends JpaRepository<BookingRoomEntity, Long> {

    Optional<BookingRoomEntity> findByRoomNumber(String roomNumber);

    boolean existsByRoomCategoryId(Long roomCategoryId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r FROM BookingRoomEntity r
            WHERE r.roomCategoryId = :roomCategoryId
              AND r.status = :status
              AND NOT EXISTS (
                  SELECT block FROM BookingRoomBlockEntity block
                  WHERE block.room = r
                    AND block.fromDate < :checkOutDate
                    AND block.toDate > :checkInDate
              )
              AND NOT EXISTS (
                  SELECT booking FROM BookingEntity booking
                  WHERE booking.room = r
                    AND (:bookingId IS NULL OR booking.id <> :bookingId)
                    AND booking.status IN :blockingStatuses
                    AND booking.checkInDate < :checkOutDate
                    AND booking.checkOutDate > :checkInDate
              )
            ORDER BY r.floor ASC, r.roomNumber ASC
            """)
    List<BookingRoomEntity> findAvailableRoomsForUpdate(
            @Param("roomCategoryId") Long roomCategoryId,
            @Param("status") RoomOperationalStatus status,
            @Param("blockingStatuses") Collection<BookingStatus> blockingStatuses,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("bookingId") UUID bookingId
    );

    @Query("""
            SELECT r FROM BookingRoomEntity r
            WHERE r.roomCategoryId = :roomCategoryId
              AND r.status = :status
              AND NOT EXISTS (
                  SELECT block FROM BookingRoomBlockEntity block
                  WHERE block.room = r
                    AND block.fromDate < :checkOutDate
                    AND block.toDate > :checkInDate
              )
              AND NOT EXISTS (
                  SELECT booking FROM BookingEntity booking
                  WHERE booking.room = r
                    AND (:bookingId IS NULL OR booking.id <> :bookingId)
                    AND booking.status IN :blockingStatuses
                    AND booking.checkInDate < :checkOutDate
                    AND booking.checkOutDate > :checkInDate
              )
            ORDER BY r.floor ASC, r.roomNumber ASC
            """)
    List<BookingRoomEntity> findAvailableRooms(
            @Param("roomCategoryId") Long roomCategoryId,
            @Param("status") RoomOperationalStatus status,
            @Param("blockingStatuses") Collection<BookingStatus> blockingStatuses,
            @Param("checkInDate") LocalDate checkInDate,
            @Param("checkOutDate") LocalDate checkOutDate,
            @Param("bookingId") UUID bookingId
    );
}
