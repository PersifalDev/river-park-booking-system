package ru.haritonenko.bookingservice.domain.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.haritonenko.bookingservice.api.dto.BookingRequestDto;
import ru.haritonenko.bookingservice.api.dto.TariffResponseDto;
import ru.haritonenko.bookingservice.config.tariff.BookingTariffProperties;
import ru.haritonenko.bookingservice.domain.db.entity.BookingEntity;
import ru.haritonenko.bookingservice.domain.db.entity.BookingTariffEntity;
import ru.haritonenko.bookingservice.domain.db.repository.BookingTariffRepository;
import ru.haritonenko.bookingservice.domain.exception.BookingTariffNotApplicableException;
import ru.haritonenko.bookingservice.domain.exception.BookingTariffNotFoundException;
import ru.haritonenko.bookingservice.domain.tariff.TariffSearchContext;
import ru.haritonenko.bookingservice.domain.tariff.price.TariffPriceModifierStrategy;
import ru.haritonenko.bookingservice.domain.tariff.rule.TariffApplicabilityRule;
import ru.haritonenko.bookingservice.external.client.catalog.CatalogServiceHttpClient;
import ru.haritonenko.commonlibs.dto.category.RoomCategoryResponseDto;
import ru.haritonenko.commonlibs.exception.RoomCategoryNotFoundException;

import java.math.BigDecimal;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class BookingTariffService {

    private final BookingTariffRepository tariffRepository;
    private final List<TariffApplicabilityRule> applicabilityRules;
    private final List<TariffPriceModifierStrategy> priceModifierStrategies;
    private final CatalogServiceHttpClient catalogServiceHttpClient;
    private final BookingTariffProperties properties;

    @Transactional(readOnly = true)
    public List<TariffResponseDto> findApplicableTariffs(BookingRequestDto request) {
        RoomCategoryResponseDto category = loadCategory(request.categoryId());
        TariffSearchContext context = context(request);
        BigDecimal basePrice = category.basePrice().multiply(BigDecimal.valueOf(context.nights()));

        return tariffRepository.findAllByActiveTrueOrderBySortOrderAscTitleAsc().stream()
                .filter(tariff -> applies(tariff, context))
                .map(tariff -> toDto(tariff, calculateTariffPrice(basePrice, context.nights(), tariff)))
                .toList();
    }

    @Transactional(readOnly = true)
    public BookingTariffEntity requireApplicableTariff(BookingEntity booking) {
        String tariffCode = booking.getTariffCode() == null || booking.getTariffCode().isBlank()
                ? properties.getDefaultCode()
                : booking.getTariffCode();
        BookingTariffEntity tariff = tariffRepository.findByCodeAndActiveTrue(tariffCode)
                .orElseThrow(() -> new BookingTariffNotFoundException("Tariff not found code=%s".formatted(tariffCode)));

        TariffSearchContext context = context(booking);
        if (!applies(tariff, context)) {
            throw new BookingTariffNotApplicableException("Tariff %s is not applicable for booking %s".formatted(tariffCode, booking.getId()));
        }
        return tariff;
    }

    public BigDecimal calculateTariffPrice(BigDecimal basePrice, long nights, BookingTariffEntity tariff) {
        BigDecimal price = priceModifierStrategies.stream()
                .filter(strategy -> strategy.supports(tariff.getPriceModifierType()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No tariff price strategy for " + tariff.getPriceModifierType()))
                .apply(basePrice, nights, tariff);
        return price.setScale(properties.getMoneyScale(), properties.getRoundingMode());
    }

    public TariffResponseDto toDto(BookingTariffEntity tariff, BigDecimal priceAmount) {
        return new TariffResponseDto(
                tariff.getCode(),
                tariff.getTitle(),
                tariff.getDescription(),
                priceAmount,
                tariff.getPriceModifierType(),
                tariff.getPriceModifierValue(),
                tariff.getCancellationPolicy(),
                tariff.getFreeCancellationDaysBefore(),
                tariff.getIncludedServices(),
                tariff.getMinNights(),
                tariff.getMaxNights(),
                tariff.getMinAdults(),
                tariff.getMinChildren()
        );
    }

    private boolean applies(BookingTariffEntity tariff, TariffSearchContext context) {
        return Boolean.TRUE.equals(tariff.getActive())
                && applicabilityRules.stream().allMatch(rule -> rule.applies(tariff, context));
    }

    private TariffSearchContext context(BookingRequestDto request) {
        return new TariffSearchContext(
                request.checkInDate(),
                request.checkOutDate(),
                request.guests(),
                request.adultCount(),
                request.childrenCount(),
                ChronoUnit.DAYS.between(request.checkInDate(), request.checkOutDate())
        );
    }

    private TariffSearchContext context(BookingEntity booking) {
        return new TariffSearchContext(
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getGuests(),
                booking.getAdultCount(),
                booking.getChildrenCount(),
                ChronoUnit.DAYS.between(booking.getCheckInDate(), booking.getCheckOutDate())
        );
    }

    private RoomCategoryResponseDto loadCategory(Long categoryId) {
        RoomCategoryResponseDto category = catalogServiceHttpClient.getRoomCategoryById(categoryId);
        if (category == null) {
            throw new RoomCategoryNotFoundException("Room category not found id=%s".formatted(categoryId));
        }
        return category;
    }
}
