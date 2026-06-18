package ru.haritonenko.telegrambot.config;

import jakarta.validation.constraints.NotBlank;

public record BotKeyboardMessagesProperties(
        @NotBlank String pickRoom,
        @NotBlank String allRooms,
        @NotBlank String myBookings,
        @NotBlank String notifications,
        @NotBlank String findRoom,
        @NotBlank String services,
        @NotBlank String findService,
        @NotBlank String rules,
        @NotBlank String contacts,
        @NotBlank String site,
        @NotBlank String noCategory,
        @NotBlank String menu,
        @NotBlank String details,
        @NotBlank String photo,
        @NotBlank String book,
        @NotBlank String backToRooms,
        @NotBlank String backToSelection,
        @NotBlank String backToServices,
        @NotBlank String sendPdf,
        @NotBlank String inactiveBookings,
        @NotBlank String earlyBookings,
        @NotBlank String bookingHistory,
        @NotBlank String backToActiveBookings,
        @NotBlank String confirmBooking,
        @NotBlank String cancelBooking,
        @NotBlank String backToBookings,
        @NotBlank String previous,
        @NotBlank String next
) {
}
