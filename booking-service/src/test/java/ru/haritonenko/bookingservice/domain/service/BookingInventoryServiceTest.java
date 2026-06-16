package ru.haritonenko.bookingservice.domain.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.haritonenko.bookingservice.domain.db.entity.BookingEntity;
import ru.haritonenko.bookingservice.domain.db.entity.BookingInventoryEntity;
import ru.haritonenko.bookingservice.domain.db.repository.BookingInventoryRepository;
import ru.haritonenko.bookingservice.domain.exception.BookingAvailabilityException;
import ru.haritonenko.bookingservice.domain.exception.BookingNotFoundException;
import ru.haritonenko.bookingservice.external.client.catalog.CatalogServiceHttpClient;
import ru.haritonenko.commonlibs.dto.category.RoomCategoryResponseDto;
import ru.haritonenko.commonlibs.dto.category.type.RoomType;
import ru.haritonenko.commonlibs.exception.RoomCategoryNotFoundException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BookingInventoryServiceTest {

    private final BookingInventoryRepository inventoryRepository = mock(BookingInventoryRepository.class);
    private final CatalogServiceHttpClient catalogServiceHttpClient = mock(CatalogServiceHttpClient.class);
    private final org.springframework.transaction.support.TransactionTemplate transactionTemplate =
            mock(org.springframework.transaction.support.TransactionTemplate.class);

    private final BookingInventoryService service =
            new BookingInventoryService(inventoryRepository, catalogServiceHttpClient, transactionTemplate);

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> {
            java.util.function.Consumer<org.springframework.transaction.TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    void shouldHoldInventoryForEveryNight() {
        BookingEntity booking = booking();
        List<BookingInventoryEntity> inventory = List.of(
                inventory(booking.getCheckInDate(), 2, 0, 0),
                inventory(booking.getCheckInDate().plusDays(1), 2, 1, 0)
        );
        when(inventoryRepository.findForUpdateByRoomCategoryIdAndBookingDateBetween(
                booking.getRoomCategoryId(),
                booking.getCheckInDate(),
                booking.getCheckOutDate()
        )).thenReturn(inventory);

        service.holdInventory(booking, 2);

        assertEquals(1, inventory.get(0).getHeldUnits());
        assertEquals(2, inventory.get(1).getHeldUnits());
        verify(inventoryRepository).insertMissingRows(1L, booking.getCheckInDate(), booking.getCheckOutDate(), 2);
    }

    @Test
    void shouldRejectHoldWhenInventoryUnavailable() {
        BookingEntity booking = booking();
        when(inventoryRepository.findForUpdateByRoomCategoryIdAndBookingDateBetween(any(), any(), any()))
                .thenReturn(List.of(inventory(booking.getCheckInDate(), 1, 1, 0)));

        assertThrows(BookingAvailabilityException.class, () -> service.holdInventory(booking, 1));
    }

    @Test
    void shouldRejectNullBookingOnHold() {
        assertThrows(BookingNotFoundException.class, () -> service.holdInventory(null, 1));
    }

    @Test
    void shouldConfirmHeldInventory() {
        BookingEntity booking = booking();
        BookingInventoryEntity inventory = inventory(booking.getCheckInDate(), 2, 1, 0);
        when(inventoryRepository.findForUpdateByRoomCategoryIdAndBookingDateBetween(any(), any(), any()))
                .thenReturn(List.of(inventory));

        service.confirmHeldInventory(booking);

        assertEquals(0, inventory.getHeldUnits());
        assertEquals(1, inventory.getConfirmedUnits());
    }

    @Test
    void shouldRejectConfirmWithoutHeldUnits() {
        BookingEntity booking = booking();
        when(inventoryRepository.findForUpdateByRoomCategoryIdAndBookingDateBetween(any(), any(), any()))
                .thenReturn(List.of(inventory(booking.getCheckInDate(), 2, 0, 0)));

        assertThrows(BookingAvailabilityException.class, () -> service.confirmHeldInventory(booking));
    }

    @Test
    void shouldReleaseHeldInventory() {
        BookingEntity booking = booking();
        BookingInventoryEntity inventory = inventory(booking.getCheckInDate(), 2, 1, 0);
        when(inventoryRepository.findForUpdateByRoomCategoryIdAndBookingDateBetween(any(), any(), any()))
                .thenReturn(List.of(inventory));

        service.releaseHeldInventory(booking);

        assertEquals(0, inventory.getHeldUnits());
    }

    @Test
    void shouldReturnUnavailableWhenAnyDateHasNoUnits() {
        BookingEntity booking = booking();
        when(inventoryRepository.findByRoomCategoryIdAndBookingDate(eq(1L), any()))
                .thenReturn(Optional.of(inventory(booking.getCheckInDate(), 1, 1, 0)));

        assertFalse(service.isAvailable(booking));
    }

    @Test
    void shouldReturnMinAvailableUnitsAcrossPeriod() {
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        when(inventoryRepository.findByRoomCategoryIdAndBookingDate(1L, checkInDate))
                .thenReturn(Optional.of(inventory(checkInDate, 5, 1, 1)));
        when(inventoryRepository.findByRoomCategoryIdAndBookingDate(1L, checkInDate.plusDays(1)))
                .thenReturn(Optional.of(inventory(checkInDate.plusDays(1), 5, 3, 0)));

        int actual = service.getAvailableUnitsForCategory(1L, checkInDate, checkInDate.plusDays(2), 5);

        assertEquals(2, actual);
    }

    @Test
    void shouldUseFallbackTotalUnitsWhenInventoryRowsMissing() {
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        when(inventoryRepository.findByRoomCategoryIdAndBookingDate(eq(1L), any())).thenReturn(Optional.empty());

        int actual = service.getAvailableUnitsForCategory(1L, checkInDate, checkInDate.plusDays(2), 7);

        assertEquals(7, actual);
    }

    @Test
    void shouldReturnTotalUnitsFromCatalog() {
        when(catalogServiceHttpClient.getRoomCategoryById(1L)).thenReturn(room(30));

        assertEquals(30, service.getTotalUnitsFromRoomCategory(1L));
    }

    @Test
    void shouldRejectMissingCategoryWhenResolvingTotalUnits() {
        when(catalogServiceHttpClient.getRoomCategoryById(1L)).thenReturn(null);

        assertThrows(RoomCategoryNotFoundException.class, () -> service.getTotalUnitsFromRoomCategory(1L));
    }

    @Test
    void shouldTreatEmptyInventoryAsAvailable() {
        BookingEntity booking = booking();
        when(inventoryRepository.findByRoomCategoryIdAndBookingDate(eq(1L), any())).thenReturn(Optional.empty());

        assertTrue(service.isAvailable(booking));
    }

    private BookingEntity booking() {
        LocalDate checkInDate = LocalDate.now().plusDays(1);
        return BookingEntity.builder()
                .id(UUID.randomUUID())
                .userId(10L)
                .roomCategoryId(1L)
                .bookingCode("BK-TEST")
                .checkInDate(checkInDate)
                .checkOutDate(checkInDate.plusDays(2))
                .build();
    }

    private BookingInventoryEntity inventory(LocalDate date, int totalUnits, int heldUnits, int confirmedUnits) {
        return BookingInventoryEntity.builder()
                .roomCategoryId(1L)
                .bookingDate(date)
                .totalUnits(totalUnits)
                .heldUnits(heldUnits)
                .confirmedUnits(confirmedUnits)
                .build();
    }

    private RoomCategoryResponseDto room(Integer totalUnits) {
        return new RoomCategoryResponseDto(
                1L,
                RoomType.STANDARD,
                "Standard room",
                2,
                BigDecimal.valueOf(5000),
                20.0,
                totalUnits,
                null,
                null
        );
    }
}
