package ru.haritonenko.bookingservice.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import ru.haritonenko.bookingservice.domain.db.entity.BookingEntity;
import ru.haritonenko.bookingservice.domain.db.entity.BookingInventoryEntity;
import ru.haritonenko.bookingservice.domain.db.repository.BookingInventoryRepository;
import ru.haritonenko.bookingservice.domain.exception.BookingAvailabilityException;
import ru.haritonenko.bookingservice.domain.exception.BookingNotFoundException;
import ru.haritonenko.bookingservice.external.client.catalog.CatalogServiceHttpClient;
import ru.haritonenko.commonlibs.exception.RoomCategoryNotFoundException;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.isNull;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingInventoryService {

    private final BookingInventoryRepository inventoryRepository;
    private final CatalogServiceHttpClient catalogServiceHttpClient;
    private final TransactionTemplate transactionTemplate;

    public void holdInventory(BookingEntity booking) {
        if (isNull(booking)) {
            log.warn("Booking not found to hold inventory");
            throw new BookingNotFoundException("Booking not found");
        }

        Long roomCategoryId = booking.getRoomCategoryId();
        Integer totalUnits = getTotalUnitsFromRoomCategory(roomCategoryId);

        log.info("Holding inventory for booking: bookingId={}, userId={}, roomCategoryId={}, bookingCode={}",
                booking.getId(),
                booking.getUserId(),
                roomCategoryId,
                booking.getBookingCode()
        );


        transactionTemplate.executeWithoutResult(status -> {
            List<BookingInventoryEntity> inventoryEntities = getOrCreateForUpdate(booking, totalUnits);
            for (BookingInventoryEntity inventory : inventoryEntities) {
                int available = inventory.getTotalUnits() - inventory.getHeldUnits() - inventory.getConfirmedUnits();
                if (available <= 0) {
                    log.warn("Inventory is not available: bookingId={}, roomCategoryId={}, bookingDate={}",
                            booking.getId(),
                            roomCategoryId,
                            inventory.getBookingDate()
                    );
                    throw new BookingAvailabilityException(
                            "No available rooms for category=%s on date=%s".formatted(roomCategoryId, inventory.getBookingDate())
                    );
                }
                inventory.setHeldUnits(inventory.getHeldUnits() + 1);
                log.info("Inventory held: bookingId={}, roomCategoryId={}, bookingDate={}, heldUnits={}",
                        booking.getId(),
                        roomCategoryId,
                        inventory.getBookingDate(),
                        inventory.getHeldUnits()
                );
            }
            inventoryRepository.saveAll(inventoryEntities);
        });
    }

    @Transactional
    public void releaseHeldInventory(BookingEntity booking) {
        log.info("Releasing held inventory for booking: bookingId={}, roomCategoryId={}",
                booking.getId(),
                booking.getRoomCategoryId()
        );

        List<BookingInventoryEntity> inventoryEntities = inventoryRepository.findForUpdateByRoomCategoryIdAndBookingDateBetween(
                booking.getRoomCategoryId(),
                booking.getCheckInDate(),
                booking.getCheckOutDate()
        );

        for (BookingInventoryEntity inventory : inventoryEntities) {
            if (inventory.getHeldUnits() > 0) {
                inventory.setHeldUnits(inventory.getHeldUnits() - 1);
                log.info("Held inventory released: bookingId={}, bookingDate={}, heldUnits={}",
                        booking.getId(),
                        inventory.getBookingDate(),
                        inventory.getHeldUnits()
                );
            }
        }
        inventoryRepository.saveAll(inventoryEntities);
    }

    @Transactional
    public void releaseConfirmedInventory(BookingEntity booking) {
        log.info("Releasing confirmed inventory for booking: bookingId={}, roomCategoryId={}",
                booking.getId(),
                booking.getRoomCategoryId()
        );

        List<BookingInventoryEntity> inventoryEntities = inventoryRepository.findForUpdateByRoomCategoryIdAndBookingDateBetween(
                booking.getRoomCategoryId(),
                booking.getCheckInDate(),
                booking.getCheckOutDate()
        );

        for (BookingInventoryEntity inventory : inventoryEntities) {
            if (inventory.getConfirmedUnits() > 0) {
                inventory.setConfirmedUnits(inventory.getConfirmedUnits() - 1);
                log.info("Confirmed inventory released: bookingId={}, bookingDate={}, confirmedUnits={}",
                        booking.getId(),
                        inventory.getBookingDate(),
                        inventory.getConfirmedUnits()
                );
            }
        }
        inventoryRepository.saveAll(inventoryEntities);
    }

    @Transactional
    public void confirmHeldInventory(BookingEntity booking) {
        log.info("Confirming held inventory for booking: bookingId={}, roomCategoryId={}",
                booking.getId(),
                booking.getRoomCategoryId()
        );

        List<BookingInventoryEntity> inventoryEntities = inventoryRepository.findForUpdateByRoomCategoryIdAndBookingDateBetween(
                booking.getRoomCategoryId(),
                booking.getCheckInDate(),
                booking.getCheckOutDate()
        );

        for (BookingInventoryEntity inventory : inventoryEntities) {
            if (inventory.getHeldUnits() <= 0) {
                log.warn("Held units not found for booking confirmation: bookingId={}, bookingDate={}",
                        booking.getId(),
                        inventory.getBookingDate()
                );
                throw new BookingAvailabilityException(
                        "Held units not found for booking confirmation id=%s".formatted(booking.getId())
                );
            }
            inventory.setHeldUnits(inventory.getHeldUnits() - 1);
            inventory.setConfirmedUnits(inventory.getConfirmedUnits() + 1);
            log.info("Held inventory confirmed: bookingId={}, bookingDate={}, heldUnits={}, confirmedUnits={}",
                    booking.getId(),
                    inventory.getBookingDate(),
                    inventory.getHeldUnits(),
                    inventory.getConfirmedUnits()
            );
        }
        inventoryRepository.saveAll(inventoryEntities);
    }

    @Transactional(readOnly = true)
    public boolean isAvailable(BookingEntity booking) {
        log.info("Checking inventory availability for booking: bookingId={}, roomCategoryId={}",
                booking.getId(),
                booking.getRoomCategoryId()
        );

        List<LocalDate> dates = getDates(booking.getCheckInDate(), booking.getCheckOutDate());
        for (LocalDate date : dates) {
            BookingInventoryEntity inventory = inventoryRepository.findByRoomCategoryIdAndBookingDate(
                    booking.getRoomCategoryId(),
                    date
            ).orElse(null);

            if (inventory == null) {
                continue;
            }

            int available = inventory.getTotalUnits() - inventory.getHeldUnits() - inventory.getConfirmedUnits();
            if (available <= 0) {
                log.info("Inventory unavailable for booking: bookingId={}, roomCategoryId={}, bookingDate={}",
                        booking.getId(),
                        booking.getRoomCategoryId(),
                        date
                );
                return false;
            }
        }

        log.info("Inventory available for booking: bookingId={}, roomCategoryId={}",
                booking.getId(),
                booking.getRoomCategoryId()
        );
        return true;
    }


    @Transactional(readOnly = true)
    public int getAvailableUnitsForCategory(Long roomCategoryId, LocalDate checkInDate, LocalDate checkOutDate, Integer totalUnits) {
        if (roomCategoryId == null || checkInDate == null || checkOutDate == null || !checkOutDate.isAfter(checkInDate)) {
            return 0;
        }

        int fallbackTotalUnits = totalUnits == null ? 0 : totalUnits;
        int minAvailable = Integer.MAX_VALUE;

        for (LocalDate date : getDates(checkInDate, checkOutDate)) {
            BookingInventoryEntity inventory = inventoryRepository.findByRoomCategoryIdAndBookingDate(roomCategoryId, date).orElse(null);
            int available;
            if (inventory == null) {
                available = fallbackTotalUnits;
            } else {
                available = inventory.getTotalUnits() - inventory.getHeldUnits() - inventory.getConfirmedUnits();
            }

            minAvailable = Math.min(minAvailable, Math.max(available, 0));
            if (minAvailable <= 0) {
                return 0;
            }
        }

        return minAvailable == Integer.MAX_VALUE ? 0 : minAvailable;
    }

    @Transactional(readOnly = true)
    public boolean isCategoryAvailable(Long roomCategoryId, LocalDate checkInDate, LocalDate checkOutDate, Integer totalUnits) {
        if (roomCategoryId == null || checkInDate == null || checkOutDate == null || !checkOutDate.isAfter(checkInDate)) {
            return false;
        }

        int fallbackTotalUnits = totalUnits == null ? 0 : totalUnits;
        for (LocalDate date : getDates(checkInDate, checkOutDate)) {
            BookingInventoryEntity inventory = inventoryRepository.findByRoomCategoryIdAndBookingDate(roomCategoryId, date).orElse(null);
            if (inventory == null) {
                if (fallbackTotalUnits <= 0) {
                    return false;
                }
                continue;
            }
            int available = inventory.getTotalUnits() - inventory.getHeldUnits() - inventory.getConfirmedUnits();
            if (available <= 0) {
                return false;
            }
        }
        return true;
    }

    private List<BookingInventoryEntity> getOrCreateForUpdate(BookingEntity booking, Integer totalUnits) {
        List<BookingInventoryEntity> existing = inventoryRepository.findForUpdateByRoomCategoryIdAndBookingDateBetween(
                booking.getRoomCategoryId(),
                booking.getCheckInDate(),
                booking.getCheckOutDate()
        );

        Long bookingCategoryId = booking.getRoomCategoryId();
        Map<LocalDate, BookingInventoryEntity> inventoryByDate = new HashMap<>();
        for (BookingInventoryEntity entity : existing) {
            inventoryByDate.put(entity.getBookingDate(), entity);
        }

        List<LocalDate> dates = getDates(booking.getCheckInDate(), booking.getCheckOutDate());
        List<BookingInventoryEntity> result = new ArrayList<>();

        for (LocalDate date : dates) {
            BookingInventoryEntity current = inventoryByDate.get(date);
            if (current == null) {
                current = BookingInventoryEntity.builder()
                        .roomCategoryId(bookingCategoryId)
                        .bookingDate(date)
                        .totalUnits(totalUnits)
                        .heldUnits(0)
                        .confirmedUnits(0)
                        .build();

                log.info("Creating default inventory row: roomCategoryId={}, bookingDate={}, totalUnits={}",
                        bookingCategoryId,
                        date,
                        totalUnits
                );
            }
            result.add(current);
        }

        return result;
    }

    private List<LocalDate> getDates(LocalDate fromDate, LocalDate toDate) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate current = fromDate;
        while (current.isBefore(toDate)) {
            dates.add(current);
            current = current.plusDays(1);
        }
        return dates;
    }

    private Integer getTotalUnitsFromRoomCategory(Long roomCategoryId) {
        if (isNull(roomCategoryId)) {
            log.warn("Room category id from request has null value");
            throw new IllegalArgumentException("Category id from request is null");
        }

        log.info("Getting room category with id={} from catalog-service", roomCategoryId);
        var category = catalogServiceHttpClient.getRoomCategoryById(roomCategoryId);
        if (isNull(category)) {
            log.warn("Room category with id={} not found", roomCategoryId);
            throw new RoomCategoryNotFoundException("Room category not found id=%s".formatted(roomCategoryId));
        }

        log.info("Extracting total units from room category with id={}", roomCategoryId);
        return category.totalUnits() != null ? category.totalUnits() : 0;
    }
}