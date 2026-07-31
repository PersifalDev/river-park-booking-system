package ru.haritonenko.bookingservice.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import ru.haritonenko.bookingservice.api.dto.BookingRequestDto;
import ru.haritonenko.bookingservice.domain.Booking;
import ru.haritonenko.bookingservice.domain.db.entity.BookingEntity;
import ru.haritonenko.bookingservice.domain.db.repository.BookingEntityRepository;
import ru.haritonenko.bookingservice.domain.db.repository.BookingIdempotencyKeyRepository;
import ru.haritonenko.bookingservice.domain.exception.BookingNotFoundException;
import ru.haritonenko.bookingservice.domain.exception.IllegalBookingStateException;
import ru.haritonenko.bookingservice.domain.status.BookingStatus;
import ru.haritonenko.bookingservice.external.client.catalog.CatalogServiceHttpClient;
import ru.haritonenko.bookingservice.external.client.users.UserServiceHttpClient;
import ru.haritonenko.bookingservice.kafka.outbox.service.BookingOutboxService;
import ru.haritonenko.bookingservice.kafka.producer.booking.sender.KafkaBookingEventSender;
import ru.haritonenko.bookingservice.kafka.producer.notification.sender.KafkaNotificationEventSender;
import ru.haritonenko.bookingservice.tasks.domain.async.db.repository.AsyncBookingTaskEntityRepository;
import ru.haritonenko.bookingservice.tasks.domain.async.dispatcher.AsyncBookingTaskDispatcher;
import ru.haritonenko.bookingservice.tasks.domain.async.status.AsyncBookingTaskStatus;
import ru.haritonenko.bookingservice.tasks.domain.async.status.ProcessingStep;
import ru.haritonenko.commonlibs.dto.category.RoomCategoryResponseDto;
import ru.haritonenko.commonlibs.dto.category.type.RoomType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private BookingService bookingService;

    @Autowired
    private BookingEntityRepository bookingRepository;

    @Autowired
    private AsyncBookingTaskEntityRepository taskRepository;

    @Autowired
    private BookingIdempotencyKeyRepository idempotencyKeyRepository;

    @MockitoBean
    private CatalogServiceHttpClient catalogServiceHttpClient;

    @MockitoBean
    private UserServiceHttpClient userServiceHttpClient;

    @MockitoBean
    private AsyncBookingTaskDispatcher taskDispatcher;

    @MockitoBean
    private BookingInventoryService bookingInventoryService;

    @MockitoBean
    private KafkaBookingEventSender bookingEventSender;

    @MockitoBean
    private KafkaNotificationEventSender notificationEventSender;

    @MockitoBean
    private BookingOutboxService bookingOutboxService;

    @BeforeEach
    void setUp() {
        idempotencyKeyRepository.deleteAll();
        taskRepository.deleteAll();
        bookingRepository.deleteAll();
        when(catalogServiceHttpClient.getRoomCategoryById(1L)).thenReturn(room());
        doNothing().when(taskDispatcher).dispatchTask(any());
        doNothing().when(bookingInventoryService).releaseHeldInventory(any());
        doNothing().when(bookingInventoryService).releaseConfirmedInventory(any());
        doNothing().when(bookingInventoryService).confirmHeldInventory(any());
        when(bookingEventSender.sendEvent(any())).thenReturn(CompletableFuture.completedFuture(null));
        doNothing().when(bookingOutboxService).saveEvent(any());
    }

    @Test
    void shouldCreateBookingDraftAndAsyncTask() {
        Booking created = bookingService.createBooking(request(), 10L, "idem-key");

        assertNotNull(created.id());
        assertEquals(BookingStatus.CREATED, created.status());
        assertEquals(1, bookingRepository.count());
        assertEquals(1, taskRepository.count());
        assertEquals(1, idempotencyKeyRepository.count());
        var task = taskRepository.findAll().getFirst();
        assertEquals(created.id(), task.getBookingId());
        assertEquals(AsyncBookingTaskStatus.NEW, task.getStatus());
        assertEquals(ProcessingStep.VALIDATE_REQUEST, task.getProcessingStep());
        verify(taskDispatcher).dispatchTask(any());
    }

    @Test
    void shouldAlwaysReadCurrentBookingStatus() {
        BookingEntity booking = bookingRepository.saveAndFlush(bookingEntity(BookingStatus.CREATED));

        assertEquals(
                BookingStatus.CREATED,
                bookingService.getBookingByUuidAndUserId(10L, booking.getId()).status()
        );

        booking.setStatus(BookingStatus.HOLD);
        bookingRepository.saveAndFlush(booking);

        assertEquals(
                BookingStatus.HOLD,
                bookingService.getBookingByUuidAndUserId(10L, booking.getId()).status()
        );
    }

    @Test
    void shouldReturnExistingBookingForRepeatedIdempotencyKey() {
        BookingRequestDto request = request();

        Booking first = bookingService.createBooking(request, 10L, "idem-key");
        Booking second = bookingService.createBooking(request, 10L, "idem-key");

        assertEquals(first.id(), second.id());
        assertEquals(1, bookingRepository.count());
        assertEquals(1, taskRepository.count());
    }

    @Test
    void shouldCancelHoldBookingAndReleaseInventory() {
        BookingEntity booking = bookingRepository.saveAndFlush(bookingEntity(BookingStatus.HOLD));

        Booking cancelled = bookingService.cancelBookingByUuidAndUserId(booking.getId(), 10L);

        assertEquals(BookingStatus.CANCELLED, cancelled.status());
        assertNull(cancelled.holdExpiresAt());
        assertEquals("Отменено", cancelled.cancellationReason());
        verify(bookingInventoryService).releaseHeldInventory(any());
        verify(bookingOutboxService).saveEvent(any());
    }

    @Test
    void shouldConfirmHoldBookingAndMoveInventory() {
        BookingEntity booking = bookingRepository.saveAndFlush(bookingEntity(BookingStatus.HOLD));

        Booking confirmed = bookingService.confirmBookingByUuidAndUserId(booking.getId(), 10L);

        assertEquals(BookingStatus.CONFIRMED, confirmed.status());
        assertNull(confirmed.holdExpiresAt());
        verify(bookingInventoryService).confirmHeldInventory(any());
        verify(bookingOutboxService).saveEvent(any());
    }

    @Test
    void shouldRejectConfirmWhenBookingIsNotHold() {
        BookingEntity booking = bookingRepository.saveAndFlush(bookingEntity(BookingStatus.CREATED));

        assertThrows(
                IllegalBookingStateException.class,
                () -> bookingService.confirmBookingByUuidAndUserId(booking.getId(), 10L)
        );
    }

    @Test
    void shouldRejectConfirmWhenHoldExpired() {
        BookingEntity booking = bookingEntity(BookingStatus.HOLD);
        booking.setHoldExpiresAt(OffsetDateTime.now().minusMinutes(1));
        BookingEntity saved = bookingRepository.saveAndFlush(booking);

        assertThrows(
                IllegalBookingStateException.class,
                () -> bookingService.confirmBookingByUuidAndUserId(saved.getId(), 10L)
        );
    }

    @Test
    void shouldRejectCancelWhenBookingAlreadyInactive() {
        BookingEntity booking = bookingRepository.saveAndFlush(bookingEntity(BookingStatus.CANCELLED));

        assertThrows(
                IllegalBookingStateException.class,
                () -> bookingService.cancelBookingByUuidAndUserId(booking.getId(), 10L)
        );
    }

    @Test
    void shouldRejectCancelWhenBookingBelongsToAnotherUser() {
        BookingEntity booking = bookingRepository.saveAndFlush(bookingEntity(BookingStatus.HOLD));

        assertThrows(
                BookingNotFoundException.class,
                () -> bookingService.cancelBookingByUuidAndUserId(booking.getId(), 11L)
        );
    }

    private BookingRequestDto request() {
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        return BookingRequestDto.builder()
                .categoryId(1L)
                .checkInDate(checkInDate)
                .checkOutDate(checkInDate.plusDays(1))
                .guests(2)
                .adultCount(2)
                .childrenCount(0)
                .build();
    }

    private BookingEntity bookingEntity(BookingStatus status) {
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        return BookingEntity.builder()
                .userId(10L)
                .roomCategoryId(1L)
                .bookingCode("BK-" + UUID.randomUUID())
                .guests(2)
                .adultCount(2)
                .childrenCount(0)
                .checkInDate(checkInDate)
                .checkOutDate(checkInDate.plusDays(1))
                .priceAmount(BigDecimal.valueOf(5000))
                .tariffCode("ROOM_ONLY")
                .tariffTitle("Room only")
                .tariffCancellationPolicy("FLEXIBLE")
                .tariffFreeCancellationDaysBefore(1)
                .tariffIncludedServices("Accommodation")
                .holdExpiresAt(requiresHoldExpiration(status) ? OffsetDateTime.now().plusMinutes(15) : null)
                .hasPromo(false)
                .status(status)
                .build();
    }

    private boolean requiresHoldExpiration(BookingStatus status) {
        return status == BookingStatus.CREATED || status == BookingStatus.HOLD;
    }

    private RoomCategoryResponseDto room() {
        return new RoomCategoryResponseDto(
                1L,
                RoomType.STANDARD,
                "Standard room",
                2,
                BigDecimal.valueOf(5000),
                20.0,
                30,
                null,
                null
        );
    }
}
