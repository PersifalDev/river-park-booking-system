package ru.haritonenko.bookingservice.tasks.domain.async.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.haritonenko.bookingservice.cache.service.BookingCacheService;
import ru.haritonenko.bookingservice.domain.db.entity.BookingEntity;
import ru.haritonenko.bookingservice.domain.db.repository.BookingEntityRepository;
import ru.haritonenko.bookingservice.domain.exception.BookingNotFoundException;
import ru.haritonenko.bookingservice.domain.event.BookingEventFactory;
import ru.haritonenko.bookingservice.domain.service.BookingEventDeliveryService;
import ru.haritonenko.bookingservice.domain.status.BookingStatus;
import ru.haritonenko.commonlibs.dto.kafka.event.BookingKafkaEvent;
import ru.haritonenko.commonlibs.dto.kafka.event.type.BookingEventType;
import ru.haritonenko.commonlibs.dto.kafka.payload.BookingKafkaPayload;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingTaskStateServiceTest {

    private final BookingEntityRepository bookingRepository = mock(BookingEntityRepository.class);
    private final BookingEventDeliveryService eventDeliveryService = mock(BookingEventDeliveryService.class);
    private final BookingEventFactory eventFactory = mock(BookingEventFactory.class);
    private final BookingCacheService cacheService = mock(BookingCacheService.class);

    private final BookingTaskStateService service =
            new BookingTaskStateService(bookingRepository, eventDeliveryService, eventFactory, cacheService);

    @BeforeEach
    void setUp() {
        when(eventFactory.bookingEvent(any(BookingEntity.class), any(BookingEventType.class)))
                .thenAnswer(invocation -> {
                    BookingEntity booking = invocation.getArgument(0);
                    BookingEventType eventType = invocation.getArgument(1);
                    return BookingKafkaEvent.<ru.haritonenko.commonlibs.dto.kafka.payload.BookingKafkaPayload>builder()
                            .eventId(UUID.randomUUID())
                            .eventType(eventType)
                            .source("booking-service-test")
                            .correlationId(booking.getId().toString())
                            .createdAt(OffsetDateTime.now())
                            .payload(ru.haritonenko.commonlibs.dto.kafka.payload.BookingKafkaPayload.builder()
                                    .bookingId(booking.getId())
                                    .build())
                            .build();
                });
    }

    @Test
    void shouldFindBookingEntity() {
        BookingEntity booking = booking(BookingStatus.CREATED);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        assertEquals(booking, service.findBookingEntity(booking.getId()));
    }

    @Test
    void shouldThrowWhenBookingNotFound() {
        UUID bookingId = UUID.randomUUID();
        when(bookingRepository.findById(bookingId)).thenReturn(Optional.empty());

        assertThrows(BookingNotFoundException.class, () -> service.findBookingEntity(bookingId));
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldMarkBookingFailedAndSendEvent() {
        BookingEntity booking = booking(BookingStatus.CREATED);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(BookingEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.markBookingFailed(booking.getId(), "reason");

        assertEquals(BookingStatus.FAILED, booking.getStatus());
        assertEquals("reason", booking.getCancellationReason());
        assertNull(booking.getHoldExpiresAt());
        verify(eventDeliveryService).publish(argThat((BookingKafkaEvent<BookingKafkaPayload> event) ->
                event.eventType() == BookingEventType.BOOKING_FAILED
        ));
    }

    @Test
    void shouldUpdateBookingPrice() {
        BookingEntity booking = booking(BookingStatus.CREATED);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));

        service.updateBookingPrice(booking.getId(), BigDecimal.valueOf(9000));

        assertEquals(0, BigDecimal.valueOf(9000).compareTo(booking.getPriceAmount()));
        verify(bookingRepository).save(booking);
    }

    @Test
    void shouldSetBookingHoldAndSendEvent() {
        BookingEntity booking = booking(BookingStatus.CREATED);
        OffsetDateTime holdExpiresAt = OffsetDateTime.now().plusMinutes(15);
        when(bookingRepository.findById(booking.getId())).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(BookingEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        service.setBookingHold(booking.getId(), BigDecimal.valueOf(10000), holdExpiresAt);

        assertEquals(BookingStatus.HOLD, booking.getStatus());
        assertEquals(0, BigDecimal.valueOf(10000).compareTo(booking.getPriceAmount()));
        assertEquals(holdExpiresAt, booking.getHoldExpiresAt());
        verify(eventDeliveryService).publish(argThat((BookingKafkaEvent<BookingKafkaPayload> event) ->
                event.eventType() == BookingEventType.BOOKING_HOLD_CREATED
        ));
    }

    private BookingEntity booking(BookingStatus status) {
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        return BookingEntity.builder()
                .id(UUID.randomUUID())
                .userId(10L)
                .roomCategoryId(1L)
                .bookingCode("BK-TEST")
                .guests(2)
                .adultCount(2)
                .childrenCount(0)
                .checkInDate(checkInDate)
                .checkOutDate(checkInDate.plusDays(1))
                .priceAmount(BigDecimal.ONE)
                .holdExpiresAt(status == BookingStatus.CREATED ? OffsetDateTime.now().plusMinutes(15) : null)
                .hasPromo(false)
                .status(status)
                .build();
    }
}
