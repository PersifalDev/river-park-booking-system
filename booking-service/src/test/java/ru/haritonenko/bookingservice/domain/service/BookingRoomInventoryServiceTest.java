package ru.haritonenko.bookingservice.domain.service;

import org.junit.jupiter.api.Test;
import ru.haritonenko.bookingservice.config.inventory.BookingRoomInventoryProperties;
import ru.haritonenko.bookingservice.domain.db.entity.BookingEntity;
import ru.haritonenko.bookingservice.domain.db.entity.BookingRoomEntity;
import ru.haritonenko.bookingservice.domain.db.repository.BookingEntityRepository;
import ru.haritonenko.bookingservice.domain.db.repository.BookingRoomRepository;
import ru.haritonenko.bookingservice.domain.exception.BookingAvailabilityException;
import ru.haritonenko.bookingservice.domain.room.RoomHousekeepingStatus;
import ru.haritonenko.bookingservice.domain.room.RoomOperationalStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class BookingRoomInventoryServiceTest {

    private final BookingRoomRepository roomRepository = mock(BookingRoomRepository.class);
    private final BookingEntityRepository bookingRepository = mock(BookingEntityRepository.class);
    private final BookingRoomInventoryProperties properties = new BookingRoomInventoryProperties(true);
    private final BookingRoomInventoryService service = new BookingRoomInventoryService(
            roomRepository,
            bookingRepository,
            properties
    );

    @Test
    void shouldUseCategoryFallbackWhenRoomsAreNotConfigured() {
        BookingEntity booking = booking();
        when(roomRepository.existsByRoomCategoryId(1L)).thenReturn(false);

        service.assignRoom(booking);

        assertNull(booking.getRoom());
        assertTrue(service.isRoomAvailable(booking));
        verifyNoInteractions(bookingRepository);
    }

    @Test
    void shouldAssignFirstAvailableRoomAndStoreSnapshot() {
        BookingEntity booking = booking();
        BookingRoomEntity room = room("301");
        when(roomRepository.existsByRoomCategoryId(1L)).thenReturn(true);
        when(roomRepository.findAvailableRoomsForUpdate(eq(1L), eq(RoomOperationalStatus.ACTIVE), any(), any(), any(), eq(booking.getId())))
                .thenReturn(List.of(room));

        service.assignRoom(booking);

        assertEquals(room, booking.getRoom());
        assertEquals("301", booking.getRoomNumberSnapshot());
        verify(bookingRepository).save(booking);
    }

    @Test
    void shouldRejectAssignmentWhenConcreteRoomsAreUnavailable() {
        BookingEntity booking = booking();
        when(roomRepository.existsByRoomCategoryId(1L)).thenReturn(true);
        when(roomRepository.findAvailableRoomsForUpdate(eq(1L), eq(RoomOperationalStatus.ACTIVE), any(), any(), any(), eq(booking.getId())))
                .thenReturn(List.of());

        assertThrows(BookingAvailabilityException.class, () -> service.assignRoom(booking));
    }

    @Test
    void shouldReturnConcreteRoomAvailability() {
        BookingEntity booking = booking();
        when(roomRepository.existsByRoomCategoryId(1L)).thenReturn(true);
        when(roomRepository.findAvailableRooms(eq(1L), eq(RoomOperationalStatus.ACTIVE), any(), any(), any(), eq(booking.getId())))
                .thenReturn(List.of(room("301")));

        assertTrue(service.isRoomAvailable(booking));
    }

    @Test
    void shouldReturnUnavailableWhenConcreteRoomsAreBusy() {
        BookingEntity booking = booking();
        when(roomRepository.existsByRoomCategoryId(1L)).thenReturn(true);
        when(roomRepository.findAvailableRooms(eq(1L), eq(RoomOperationalStatus.ACTIVE), any(), any(), any(), eq(booking.getId())))
                .thenReturn(List.of());

        assertFalse(service.isRoomAvailable(booking));
    }

    @Test
    void shouldMarkAssignedRoomDirtyAfterDeparture() {
        BookingEntity booking = booking();
        BookingRoomEntity room = room("301");
        booking.setRoom(room);

        service.markRoomDirtyAfterDeparture(booking);

        assertEquals(RoomHousekeepingStatus.DIRTY, room.getHousekeepingStatus());
        verify(roomRepository).save(room);
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

    private BookingRoomEntity room(String roomNumber) {
        return BookingRoomEntity.builder()
                .id(10L)
                .roomCategoryId(1L)
                .roomNumber(roomNumber)
                .floor(3)
                .status(RoomOperationalStatus.ACTIVE)
                .housekeepingStatus(RoomHousekeepingStatus.CLEAN)
                .build();
    }
}
