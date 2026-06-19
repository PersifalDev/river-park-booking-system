package ru.haritonenko.bookingservice.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.haritonenko.bookingservice.config.inventory.BookingRoomInventoryProperties;
import ru.haritonenko.bookingservice.domain.db.entity.BookingEntity;
import ru.haritonenko.bookingservice.domain.db.entity.BookingRoomEntity;
import ru.haritonenko.bookingservice.domain.db.repository.BookingEntityRepository;
import ru.haritonenko.bookingservice.domain.db.repository.BookingRoomRepository;
import ru.haritonenko.bookingservice.domain.exception.BookingAvailabilityException;
import ru.haritonenko.bookingservice.domain.room.RoomHousekeepingStatus;
import ru.haritonenko.bookingservice.domain.room.RoomOperationalStatus;
import ru.haritonenko.bookingservice.domain.status.BookingStatus;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingRoomInventoryService {

    private static final List<BookingStatus> ROOM_BLOCKING_STATUSES = List.of(
            BookingStatus.HOLD,
            BookingStatus.CONFIRMED
    );

    private final BookingRoomRepository roomRepository;
    private final BookingEntityRepository bookingRepository;
    private final BookingRoomInventoryProperties properties;

    @Transactional(readOnly = true)
    public boolean isRoomAvailable(BookingEntity booking) {
        if (booking == null) {
            return false;
        }
        if (isRoomInventoryNotConfigured(booking)) {
            return properties.fallbackToCategoryInventory();
        }

        return !roomRepository.findAvailableRooms(
                booking.getRoomCategoryId(),
                RoomOperationalStatus.ACTIVE,
                ROOM_BLOCKING_STATUSES,
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getId()
        ).isEmpty();
    }

    @Transactional
    public void assignRoom(BookingEntity booking) {
        if (booking == null || booking.getRoom() != null) {
            return;
        }
        if (isRoomInventoryNotConfigured(booking)) {
            if (properties.fallbackToCategoryInventory()) {
                log.info("Room-level inventory is empty for category={}, bookingId={}. Category inventory fallback is used",
                        booking.getRoomCategoryId(),
                        booking.getId()
                );
                return;
            }
            throw new BookingAvailabilityException(
                    "Room-level inventory is not configured for category=%s".formatted(booking.getRoomCategoryId())
            );
        }

        BookingRoomEntity room = roomRepository.findAvailableRoomsForUpdate(
                booking.getRoomCategoryId(),
                RoomOperationalStatus.ACTIVE,
                ROOM_BLOCKING_STATUSES,
                booking.getCheckInDate(),
                booking.getCheckOutDate(),
                booking.getId()
        ).stream().findFirst().orElseThrow(() -> new BookingAvailabilityException(
                "No available rooms for category=%s between %s and %s".formatted(
                        booking.getRoomCategoryId(),
                        booking.getCheckInDate(),
                        booking.getCheckOutDate()
                )
        ));

        booking.setRoom(room);
        booking.setRoomNumberSnapshot(room.getRoomNumber());
        bookingRepository.save(booking);
        log.info("Room assigned: bookingId={}, roomId={}, roomNumber={}",
                booking.getId(),
                room.getId(),
                room.getRoomNumber()
        );
    }

    @Transactional
    public void markRoomDirtyAfterDeparture(BookingEntity booking) {
        if (booking == null || booking.getRoom() == null) {
            return;
        }

        BookingRoomEntity room = booking.getRoom();
        room.setHousekeepingStatus(RoomHousekeepingStatus.DIRTY);
        roomRepository.save(room);
        log.info("Room marked dirty after departure: bookingId={}, roomId={}, roomNumber={}",
                booking.getId(),
                room.getId(),
                room.getRoomNumber()
        );
    }

    private boolean isRoomInventoryNotConfigured(BookingEntity booking) {
        return !roomRepository.existsByRoomCategoryId(booking.getRoomCategoryId());
    }
}
