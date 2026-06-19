package ru.haritonenko.bookingservice.config.inventory;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.booking.room-inventory")
public record BookingRoomInventoryProperties(
        boolean fallbackToCategoryInventory
) {
}
