package ru.haritonenko.telegrambot.config;

import jakarta.validation.constraints.NotBlank;

public record BotInputMessagesProperties(
        @NotBlank String roomIdMustBeNumber,
        @NotBlank String serviceIdMustBeNumber,
        @NotBlank String guestsMustBeInteger,
        @NotBlank String adultsMustBeInteger,
        @NotBlank String childrenMustBeInteger,
        @NotBlank String roomTypeNotRecognized,
        @NotBlank String minPriceMustBeNumber,
        @NotBlank String maxPriceMustBeNumber,
        @NotBlank String minAreaMustBeNumber,
        @NotBlank String maxPriceLowerThanMin
) {
}
