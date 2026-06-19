package ru.haritonenko.bookingservice.domain.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.haritonenko.bookingservice.domain.db.entity.BookingPriceCalendarEntity;

import java.time.LocalDate;
import java.util.List;

public interface BookingPriceCalendarRepository extends JpaRepository<BookingPriceCalendarEntity, Long> {

    List<BookingPriceCalendarEntity> findAllByRoomCategoryIdAndRatePlan_IdAndCalendarDateGreaterThanEqualAndCalendarDateLessThanOrderByCalendarDateAsc(
            Long roomCategoryId,
            Long ratePlanId,
            LocalDate from,
            LocalDate to
    );
}
