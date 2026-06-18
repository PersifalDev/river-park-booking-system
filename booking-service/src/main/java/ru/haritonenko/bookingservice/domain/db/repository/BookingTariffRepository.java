package ru.haritonenko.bookingservice.domain.db.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import ru.haritonenko.bookingservice.domain.db.entity.BookingTariffEntity;

import java.util.List;
import java.util.Optional;

public interface BookingTariffRepository extends JpaRepository<BookingTariffEntity, Long> {

    Optional<BookingTariffEntity> findByCodeAndActiveTrue(String code);

    List<BookingTariffEntity> findAllByActiveTrueOrderBySortOrderAscTitleAsc();
}
