package ru.haritonenko.bookingservice.tasks.domain.async.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.haritonenko.bookingservice.cache.service.BookingCacheService;
import ru.haritonenko.bookingservice.domain.db.entity.BookingEntity;
import ru.haritonenko.bookingservice.domain.db.repository.BookingEntityRepository;
import ru.haritonenko.bookingservice.domain.exception.BookingNotFoundException;
import ru.haritonenko.bookingservice.domain.status.BookingStatus;
import ru.haritonenko.bookingservice.kafka.producer.booking.sender.KafkaBookingEventSender;
import ru.haritonenko.commonlibs.dto.kafka.event.BookingKafkaEvent;
import ru.haritonenko.commonlibs.dto.kafka.event.type.BookingEventType;
import ru.haritonenko.commonlibs.dto.kafka.payload.BookingKafkaPayload;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingTaskStateService {

    private final BookingEntityRepository bookingRepository;
    private final KafkaBookingEventSender bookingEventSender;
    private final BookingCacheService cacheService;

    @Value("${app.booking.events.source}")
    private String sourceService;

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
    public void markBookingFailed(UUID bookingId, String reason) {
        log.warn("Marking booking as failed: bookingId={}, reason={}", bookingId, reason);
        BookingEntity booking = findBookingEntity(bookingId);
        booking.setStatus(BookingStatus.FAILED);
        booking.setCancellationReason(reason);
        booking.setHoldExpiresAt(null);
        BookingEntity savedBooking = bookingRepository.save(booking);

        log.info("Sending event to Kafka to mark booking as failed: eventType={}", BookingEventType.BOOKING_FAILED);
        bookingEventSender.sendEvent(toKafkaEvent(savedBooking, BookingEventType.BOOKING_FAILED));
        log.info("Booking status was updated to {} after starting marking: bookingId={}", booking.getStatus(), bookingId);
        evictBookingCaches(booking);
    }

    @Transactional
    public void updateBookingPrice(UUID bookingId, BigDecimal priceAmount) {
        log.info("Updating booking price: bookingId={}, priceAmount={}", bookingId, priceAmount);
        BookingEntity booking = findBookingEntity(bookingId);
        booking.setPriceAmount(priceAmount);
        bookingRepository.save(booking);
        log.info("Booking price was updated: bookingId={}, priceAmount={}", bookingId, priceAmount);
        evictBookingCaches(booking);
    }

    @Transactional
    public void setBookingHold(UUID bookingId, BigDecimal priceAmount, OffsetDateTime holdExpiresAt) {
        log.info("Setting booking hold: bookingId={}, holdExpiresAt={}", bookingId, holdExpiresAt);
        BookingEntity booking = findBookingEntity(bookingId);
        booking.setPriceAmount(priceAmount);
        booking.setStatus(BookingStatus.HOLD);
        booking.setHoldExpiresAt(holdExpiresAt);
        BookingEntity savedBooking = bookingRepository.save(booking);

        log.info("Sending event to Kafka to hold booking: eventType={}", BookingEventType.BOOKING_HOLD_CREATED);
        bookingEventSender.sendEvent(toKafkaEvent(savedBooking, BookingEventType.BOOKING_HOLD_CREATED));
        log.info("Booking status was updated to {} after starting holding: bookingId={}", booking.getStatus(), bookingId);
        evictBookingCaches(booking);
    }

    private void evictBookingCaches(BookingEntity booking) {
        cacheService.evictBookingByUser(booking.getUserId(), booking.getId());
        cacheService.evictUserPages(booking.getUserId());
    }

    private BookingKafkaEvent<BookingKafkaPayload> toKafkaEvent(BookingEntity booking, BookingEventType type) {
        return BookingKafkaEvent.<BookingKafkaPayload>builder()
                .eventId(UUID.randomUUID())
                .correlationId(booking.getId().toString())
                .source(sourceService)
                .eventType(type)
                .createdAt(OffsetDateTime.now())
                .payload(BookingKafkaPayload.builder()
                        .bookingId(booking.getId())
                        .bookingCode(booking.getBookingCode())
                        .userId(booking.getUserId())
                        .roomCategoryId(booking.getRoomCategoryId())
                        .guests(booking.getGuests())
                        .adultCount(booking.getAdultCount())
                        .childrenCount(booking.getChildrenCount())
                        .checkInDate(booking.getCheckInDate())
                        .checkOutDate(booking.getCheckOutDate())
                        .priceAmount(booking.getPriceAmount())
                        .bookingStatus(booking.getStatus().name())
                        .holdExpiresAt(booking.getHoldExpiresAt())
                        .cancellationReason(booking.getCancellationReason())
                        .build())
                .build();
    }
}
