package ru.haritonenko.bookingservice.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.haritonenko.bookingservice.domain.db.entity.BookingEntity;
import ru.haritonenko.bookingservice.external.client.catalog.CatalogServiceHttpClient;
import ru.haritonenko.commonlibs.exception.RoomCategoryNotFoundException;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;

import static java.util.Objects.isNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingPricingService {

    private final CatalogServiceHttpClient catalogServiceHttpClient;

    public BigDecimal calculatePrice(BookingEntity booking) {
        long nights = ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate());
        Long roomCategoryId = booking.getRoomCategoryId();
        if (isNull(roomCategoryId)) {
            log.warn("Room category id from request has null value");
            throw new IllegalArgumentException("Category id from request is null");
        }
        var category = catalogServiceHttpClient.getRoomCategoryById(roomCategoryId);
        if (isNull(category)) {
            log.warn("Room category from request to catalog service is null: booking id={}",
                    booking.getId());
            throw new RoomCategoryNotFoundException("Room category not found");
        }
        BigDecimal totalPrice = category.basePrice().multiply(BigDecimal.valueOf(nights));
        log.info("Calculated booking price: bookingId={}, roomCategoryId={}, nights={}, totalPrice={}",
                booking.getId(),
                roomCategoryId,
                nights,
                totalPrice
        );
        return totalPrice;
    }
}
