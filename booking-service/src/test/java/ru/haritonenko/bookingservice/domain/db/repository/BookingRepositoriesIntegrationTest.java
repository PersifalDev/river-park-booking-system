package ru.haritonenko.bookingservice.domain.db.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;
import ru.haritonenko.bookingservice.domain.service.AbstractIntegrationTest;
import ru.haritonenko.bookingservice.domain.db.entity.BookingEntity;
import ru.haritonenko.bookingservice.domain.db.entity.BookingIdempotencyKeyEntity;
import ru.haritonenko.bookingservice.domain.db.entity.BookingInventoryEntity;
import ru.haritonenko.bookingservice.domain.db.entity.BookingRoomEntity;
import ru.haritonenko.bookingservice.domain.db.entity.PromoCodeEntity;
import ru.haritonenko.bookingservice.domain.room.RoomHousekeepingStatus;
import ru.haritonenko.bookingservice.domain.room.RoomOperationalStatus;
import ru.haritonenko.bookingservice.domain.status.BookingStatus;
import ru.haritonenko.bookingservice.external.client.catalog.CatalogServiceHttpClient;
import ru.haritonenko.bookingservice.external.client.users.UserServiceHttpClient;
import ru.haritonenko.bookingservice.kafka.outbox.service.BookingOutboxService;
import ru.haritonenko.bookingservice.kafka.producer.booking.sender.KafkaBookingEventSender;
import ru.haritonenko.bookingservice.kafka.producer.notification.sender.KafkaNotificationEventSender;
import ru.haritonenko.bookingservice.tasks.domain.async.db.entity.AsyncBookingTaskEntity;
import ru.haritonenko.bookingservice.tasks.domain.async.db.repository.AsyncBookingTaskEntityRepository;
import ru.haritonenko.bookingservice.tasks.domain.async.status.AsyncBookingTaskStatus;
import ru.haritonenko.bookingservice.tasks.domain.async.status.ProcessingStep;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class BookingRepositoriesIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private BookingEntityRepository bookingRepository;

    @Autowired
    private BookingInventoryRepository inventoryRepository;

    @Autowired
    private BookingRoomRepository roomRepository;

    @Autowired
    private BookingRoomBlockRepository roomBlockRepository;

    @Autowired
    private BookingIdempotencyKeyRepository idempotencyKeyRepository;

    @Autowired
    private PromoCodeRepository promoCodeRepository;

    @Autowired
    private AsyncBookingTaskEntityRepository taskRepository;

    @MockitoBean
    private CatalogServiceHttpClient catalogServiceHttpClient;

    @MockitoBean
    private UserServiceHttpClient userServiceHttpClient;

    @MockitoBean
    private KafkaBookingEventSender bookingEventSender;

    @MockitoBean
    private KafkaNotificationEventSender notificationEventSender;

    @MockitoBean
    private BookingOutboxService bookingOutboxService;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
        idempotencyKeyRepository.deleteAll();
        promoCodeRepository.deleteAll();
        inventoryRepository.deleteAll();
        bookingRepository.deleteAll();
        roomBlockRepository.deleteAll();
        roomRepository.deleteAll();
        bookingRepository.flush();
    }

    @Test
    void shouldFindBookingByIdAndUserId() {
        BookingEntity booking = booking(1L, BookingStatus.HOLD, OffsetDateTime.now().plusMinutes(15));
        BookingEntity saved = bookingRepository.saveAndFlush(booking);

        assertTrue(bookingRepository.findByIdAndUserId(saved.getId(), 1L).isPresent());
        assertTrue(bookingRepository.findByIdAndUserId(saved.getId(), 2L).isEmpty());
    }

    @Test
    void shouldFindActiveAndInactiveBookingsByUser() {
        bookingRepository.save(booking(1L, BookingStatus.HOLD, OffsetDateTime.now().plusMinutes(15)));
        bookingRepository.save(booking(1L, BookingStatus.CONFIRMED, null));
        bookingRepository.save(booking(1L, BookingStatus.CANCELLED, null));
        bookingRepository.save(booking(2L, BookingStatus.HOLD, OffsetDateTime.now().plusMinutes(15)));
        bookingRepository.flush();

        assertEquals(2, bookingRepository.findAllByUserIdAndStatusIn(
                1L,
                List.of(BookingStatus.HOLD, BookingStatus.CONFIRMED),
                PageRequest.of(0, 10)
        ).getTotalElements());
        assertEquals(1, bookingRepository.findAllByUserIdAndStatusIn(
                1L,
                List.of(BookingStatus.CANCELLED, BookingStatus.EXPIRED, BookingStatus.FAILED),
                PageRequest.of(0, 10)
        ).getTotalElements());
    }

    @Test
    void shouldFindExpiredHoldsAndCreatedDrafts() {
        bookingRepository.save(booking(1L, BookingStatus.HOLD, OffsetDateTime.now().minusMinutes(1)));
        bookingRepository.save(booking(1L, BookingStatus.CREATED, OffsetDateTime.now().minusMinutes(1)));
        bookingRepository.save(booking(1L, BookingStatus.HOLD, OffsetDateTime.now().plusMinutes(10)));
        bookingRepository.flush();

        assertEquals(1, bookingRepository.findExpiredHolds(BookingStatus.HOLD, OffsetDateTime.now()).size());
        assertEquals(1, bookingRepository.findExpiredCreatedDrafts(BookingStatus.CREATED, OffsetDateTime.now()).size());
    }

    @Test
    void shouldInsertAndFindInventoryRowsForPeriod() {
        LocalDate checkInDate = LocalDate.now().plusDays(1);

        inventoryRepository.insertMissingRows(1L, checkInDate, checkInDate.plusDays(3), 10);
        inventoryRepository.flush();

        List<BookingInventoryEntity> rows = inventoryRepository.findForUpdateByRoomCategoryIdAndBookingDateBetween(
                1L,
                checkInDate,
                checkInDate.plusDays(3)
        );
        assertEquals(3, rows.size());
        assertEquals(checkInDate, rows.getFirst().getBookingDate());
        assertEquals(10, rows.getFirst().getTotalUnits());
    }

    @Test
    void shouldFindAvailableConcreteRoomsForPeriod() {
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        BookingRoomEntity occupiedRoom = room("301");
        BookingRoomEntity availableRoom = room("302");
        roomRepository.saveAllAndFlush(List.of(occupiedRoom, availableRoom));

        BookingEntity booking = booking(1L, BookingStatus.HOLD, OffsetDateTime.now().plusMinutes(15));
        booking.setRoom(occupiedRoom);
        booking.setRoomNumberSnapshot(occupiedRoom.getRoomNumber());
        booking.setCheckInDate(checkInDate);
        booking.setCheckOutDate(checkInDate.plusDays(2));
        bookingRepository.saveAndFlush(booking);

        List<BookingRoomEntity> availableRooms = roomRepository.findAvailableRoomsForUpdate(
                1L,
                RoomOperationalStatus.ACTIVE,
                List.of(BookingStatus.HOLD, BookingStatus.CONFIRMED),
                checkInDate,
                checkInDate.plusDays(2),
                UUID.randomUUID()
        );

        assertEquals(1, availableRooms.size());
        assertEquals("302", availableRooms.getFirst().getRoomNumber());
    }

    @Test
    void shouldFindActiveIdempotencyKeyOnlyBeforeExpiration() {
        UUID bookingId = bookingRepository.saveAndFlush(
                booking(1L, BookingStatus.CREATED, OffsetDateTime.now().plusMinutes(15))
        ).getId();
        idempotencyKeyRepository.saveAndFlush(BookingIdempotencyKeyEntity.builder()
                .userId(1L)
                .idempotencyKey("key")
                .requestHash("hash")
                .bookingId(bookingId)
                .expiresAt(OffsetDateTime.now().plusHours(1))
                .build());

        assertTrue(idempotencyKeyRepository.findByUserIdAndIdempotencyKeyAndExpiresAtAfter(
                1L,
                "key",
                OffsetDateTime.now()
        ).isPresent());
        assertTrue(idempotencyKeyRepository.findByUserIdAndIdempotencyKeyAndExpiresAtAfter(
                1L,
                "key",
                OffsetDateTime.now().plusDays(2)
        ).isEmpty());
    }

    @Test
    void shouldFindUsablePromoCodeForUser() {
        UUID sourceBookingId = UUID.randomUUID();
        promoCodeRepository.saveAndFlush(PromoCodeEntity.builder()
                .code("RP-TEST")
                .userId(1L)
                .sourceBookingId(sourceBookingId)
                .discountPercent(10)
                .used(false)
                .build());

        assertTrue(promoCodeRepository.existsByCode("RP-TEST"));
        assertTrue(promoCodeRepository.existsBySourceBookingId(sourceBookingId));
        assertTrue(promoCodeRepository.findForUpdateByCodeAndUserIdAndUsedFalse("RP-TEST", 1L).isPresent());
        assertTrue(promoCodeRepository.findForUpdateByCodeAndUserIdAndUsedFalse("RP-TEST", 2L).isEmpty());
    }

    @Test
    void shouldPickTasksForProcessingByStatusAndTime() {
        AsyncBookingTaskEntity newTask = task(AsyncBookingTaskStatus.NEW, null);
        AsyncBookingTaskEntity retryReady = task(AsyncBookingTaskStatus.FAILED_RETRYABLE, OffsetDateTime.now().minusSeconds(1));
        taskRepository.save(task(AsyncBookingTaskStatus.FAILED_RETRYABLE, OffsetDateTime.now().plusMinutes(1)));
        taskRepository.save(newTask);
        taskRepository.save(retryReady);
        taskRepository.flush();

        List<AsyncBookingTaskEntity> picked = taskRepository.pickBatchForProcessing(
                AsyncBookingTaskStatus.NEW.getCode(),
                AsyncBookingTaskStatus.FAILED_RETRYABLE.getCode(),
                AsyncBookingTaskStatus.IN_PROGRESS.getCode(),
                10,
                OffsetDateTime.now()
        );

        assertEquals(2, picked.size());
    }

    private BookingEntity booking(Long userId, BookingStatus status, OffsetDateTime holdExpiresAt) {
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        return BookingEntity.builder()
                .userId(userId)
                .roomCategoryId(1L)
                .bookingCode("BK-" + UUID.randomUUID())
                .guests(2)
                .adultCount(2)
                .childrenCount(0)
                .checkInDate(checkInDate)
                .checkOutDate(checkInDate.plusDays(1))
                .priceAmount(BigDecimal.ONE)
                .holdExpiresAt(holdExpiresAt)
                .hasPromo(false)
                .status(status)
                .cancellationReason(status == BookingStatus.CANCELLED ? "Отменено" : null)
                .build();
    }

    private AsyncBookingTaskEntity task(AsyncBookingTaskStatus status, OffsetDateTime nextAttemptAt) {
        return AsyncBookingTaskEntity.builder()
                .bookingId(UUID.randomUUID())
                .status(status)
                .processingStep(ProcessingStep.VALIDATE_REQUEST)
                .attempts(0)
                .nextAttemptAt(nextAttemptAt)
                .build();
    }

    private BookingRoomEntity room(String roomNumber) {
        return BookingRoomEntity.builder()
                .roomCategoryId(1L)
                .roomNumber(roomNumber)
                .floor(3)
                .status(RoomOperationalStatus.ACTIVE)
                .housekeepingStatus(RoomHousekeepingStatus.CLEAN)
                .build();
    }
}
