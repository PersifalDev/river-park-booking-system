package ru.haritonenko.bookingservice.tasks.domain.async.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.haritonenko.bookingservice.cache.service.BookingCacheService;
import ru.haritonenko.bookingservice.domain.db.entity.BookingEntity;
import ru.haritonenko.bookingservice.domain.db.repository.BookingEntityRepository;
import ru.haritonenko.bookingservice.domain.exception.BookingNotFoundException;
import ru.haritonenko.bookingservice.domain.exception.IllegalBookingStateException;
import ru.haritonenko.bookingservice.observability.BookingMetrics;
import ru.haritonenko.bookingservice.domain.event.BookingEventFactory;
import ru.haritonenko.bookingservice.domain.service.BookingEventDeliveryService;
import ru.haritonenko.bookingservice.domain.status.BookingStatus;
import ru.haritonenko.commonlibs.dto.kafka.event.type.BookingEventType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingTaskStateService {

    private final BookingEntityRepository bookingRepository;
    private final BookingEventDeliveryService eventDeliveryService;
    private final BookingEventFactory eventFactory;
    private final BookingCacheService cacheService;
    private final BookingMetrics metrics;

    @Transactional(readOnly = true)
    public boolean existsBookingById(UUID bookingId) {
        return bookingRepository.existsById(bookingId);
    }

    @Transactional(readOnly = true)
    public BookingEntity findBookingEntity(UUID bookingId) {
        log.info("Searching booking entity by id={}", bookingId);
        return bookingRepository.findById(bookingId)
                .orElseThrow(() -> {
                    log.warn("Booking entity not found by id={}", bookingId);
                    return new BookingNotFoundException("Booking not found id=%s".formatted(bookingId));
                });
    }

    @Transactional
    public BookingEntity findBookingEntityForUpdate(UUID bookingId) {
        return bookingRepository.findByIdForUpdate(bookingId)
                .orElseThrow(() -> new BookingNotFoundException("Booking not found id=" + bookingId));
    }

    @Transactional
    public void markBookingFailed(UUID bookingId, String reason) {
        log.warn("Marking booking as failed: bookingId={}, reason={}", bookingId, reason);
        BookingEntity booking = findBookingEntityForUpdate(bookingId);
        if (booking.getStatus() != BookingStatus.CREATED) {
            return;
        }
        booking.setStatus(BookingStatus.FAILED);
        booking.setCancellationReason(reason);
        booking.setHoldExpiresAt(null);
        BookingEntity savedBooking = bookingRepository.save(booking);
        metrics.record(BookingStatus.FAILED);

        log.info("Publishing booking event: eventType={}", BookingEventType.BOOKING_FAILED);
        eventDeliveryService.submitForDelivery(eventFactory.bookingEvent(savedBooking, BookingEventType.BOOKING_FAILED));
        log.info("Booking status was updated to {} after starting marking: bookingId={}", booking.getStatus(), bookingId);
        evictBookingCaches(booking);
    }

    @Transactional
    public void updateBookingPrice(UUID bookingId, BigDecimal priceAmount) {
        log.info("Updating booking price: bookingId={}, priceAmount={}", bookingId, priceAmount);
        BookingEntity booking = findBookingEntityForUpdate(bookingId);
        requireCreated(booking);
        booking.setPriceAmount(priceAmount);
        bookingRepository.save(booking);
        log.info("Booking price was updated: bookingId={}, priceAmount={}", bookingId, priceAmount);
        evictBookingCaches(booking);
    }

    @Transactional
    public void setBookingHold(UUID bookingId, BigDecimal priceAmount, OffsetDateTime holdExpiresAt) {
        log.info("Setting booking hold: bookingId={}, holdExpiresAt={}", bookingId, holdExpiresAt);
        BookingEntity booking = findBookingEntityForUpdate(bookingId);
        if (booking.getStatus() == BookingStatus.HOLD) {
            return;
        }
        requireCreated(booking);
        booking.setPriceAmount(priceAmount);
        booking.setStatus(BookingStatus.HOLD);
        booking.setHoldExpiresAt(holdExpiresAt);
        BookingEntity savedBooking = bookingRepository.save(booking);
        metrics.record(BookingStatus.HOLD);
        metrics.recordHoldLatency(savedBooking.getCreatedAt());

        log.info("Submitting booking hold event: eventType={}", BookingEventType.BOOKING_HOLD_CREATED);
        eventDeliveryService.submitForDelivery(eventFactory.bookingEvent(savedBooking, BookingEventType.BOOKING_HOLD_CREATED));
        log.info("Booking status was updated to {} after starting holding: bookingId={}", booking.getStatus(), bookingId);
        evictBookingCaches(booking);
    }

    private void evictBookingCaches(BookingEntity booking) {
        cacheService.evictBookingByUser(booking.getUserId(), booking.getId());
        cacheService.evictUserPages(booking.getUserId());
    }

    private void requireCreated(BookingEntity booking) {
        if (booking.getStatus() != BookingStatus.CREATED) {
            throw new IllegalBookingStateException("Booking is no longer CREATED id=" + booking.getId());
        }
    }

}
