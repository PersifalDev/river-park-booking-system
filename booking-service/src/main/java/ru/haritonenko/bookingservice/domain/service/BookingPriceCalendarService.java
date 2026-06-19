package ru.haritonenko.bookingservice.domain.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.haritonenko.bookingservice.config.pricing.BookingPriceCalendarProperties;
import ru.haritonenko.bookingservice.domain.db.entity.BookingPriceCalendarEntity;
import ru.haritonenko.bookingservice.domain.db.entity.BookingTariffEntity;
import ru.haritonenko.bookingservice.domain.db.repository.BookingPriceCalendarRepository;
import ru.haritonenko.bookingservice.domain.exception.BookingPriceCalendarMissingException;
import ru.haritonenko.bookingservice.domain.exception.BookingPriceCalendarUnavailableException;
import ru.haritonenko.commonlibs.dto.category.RoomCategoryResponseDto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class BookingPriceCalendarService {

    private final BookingPriceCalendarRepository priceCalendarRepository;
    private final BookingPriceCalendarProperties properties;

    @Transactional(readOnly = true)
    public BigDecimal calculateBasePrice(
            RoomCategoryResponseDto category,
            BookingTariffEntity ratePlan,
            LocalDate checkInDate,
            LocalDate checkOutDate
    ) {
        long nights = ChronoUnit.DAYS.between(checkInDate, checkOutDate);
        if (nights <= 0) {
            throw new IllegalArgumentException("Booking period must contain at least one night");
        }

        Map<LocalDate, BookingPriceCalendarEntity> calendarByDate = priceCalendarRepository
                .findAllByRoomCategoryIdAndRatePlan_IdAndCalendarDateGreaterThanEqualAndCalendarDateLessThanOrderByCalendarDateAsc(
                        category.id(),
                        ratePlan.getId(),
                        checkInDate,
                        checkOutDate
                )
                .stream()
                .collect(Collectors.toMap(BookingPriceCalendarEntity::getCalendarDate, Function.identity()));

        validateStayRestrictions(calendarByDate, checkInDate, checkOutDate, nights);

        return stayDates(checkInDate, checkOutDate)
                .map(date -> nightlyPrice(category, ratePlan, calendarByDate.get(date), date))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(properties.getMoneyScale(), properties.getRoundingMode());
    }

    private void validateStayRestrictions(
            Map<LocalDate, BookingPriceCalendarEntity> calendarByDate,
            LocalDate checkInDate,
            LocalDate checkOutDate,
            long nights
    ) {
        BookingPriceCalendarEntity checkInRate = calendarByDate.get(checkInDate);
        if (checkInRate != null && Boolean.TRUE.equals(checkInRate.getClosedToArrival())) {
            throw new BookingPriceCalendarUnavailableException("Arrival is closed for date=%s".formatted(checkInDate));
        }

        LocalDate lastNight = checkOutDate.minusDays(1);
        BookingPriceCalendarEntity lastNightRate = calendarByDate.get(lastNight);
        if (lastNightRate != null && Boolean.TRUE.equals(lastNightRate.getClosedToDeparture())) {
            throw new BookingPriceCalendarUnavailableException("Departure is closed for date=%s".formatted(checkOutDate));
        }

        Integer requiredMinStay = calendarByDate.values()
                .stream()
                .map(BookingPriceCalendarEntity::getMinStay)
                .filter(value -> value != null && value > 0)
                .max(Integer::compareTo)
                .orElse(null);
        if (requiredMinStay != null && nights < requiredMinStay) {
            throw new BookingPriceCalendarUnavailableException("Minimum stay is %s nights".formatted(requiredMinStay));
        }
    }

    private BigDecimal nightlyPrice(
            RoomCategoryResponseDto category,
            BookingTariffEntity ratePlan,
            BookingPriceCalendarEntity calendarRate,
            LocalDate date
    ) {
        if (calendarRate == null) {
            if (properties.isFallbackToCategoryBasePrice()) {
                return category.basePrice();
            }
            throw new BookingPriceCalendarMissingException(
                    "Price calendar rate is missing: roomCategoryId=%s, ratePlan=%s, date=%s"
                            .formatted(category.id(), ratePlan.getCode(), date)
            );
        }
        if (!Boolean.TRUE.equals(calendarRate.getAvailable())) {
            throw new BookingPriceCalendarUnavailableException(
                    "Price calendar rate is unavailable: roomCategoryId=%s, ratePlan=%s, date=%s"
                            .formatted(category.id(), ratePlan.getCode(), date)
            );
        }
        BigDecimal multiplier = calendarRate.getDemandMultiplier() == null ? BigDecimal.ONE : calendarRate.getDemandMultiplier();
        return calendarRate.getPrice().multiply(multiplier);
    }

    private Stream<LocalDate> stayDates(LocalDate checkInDate, LocalDate checkOutDate) {
        return checkInDate.datesUntil(checkOutDate);
    }
}
