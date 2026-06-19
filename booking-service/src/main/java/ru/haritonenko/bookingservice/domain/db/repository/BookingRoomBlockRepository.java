package ru.haritonenko.bookingservice.domain.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.haritonenko.bookingservice.domain.db.entity.BookingRoomBlockEntity;

@Repository
public interface BookingRoomBlockRepository extends JpaRepository<BookingRoomBlockEntity, Long> {
}
