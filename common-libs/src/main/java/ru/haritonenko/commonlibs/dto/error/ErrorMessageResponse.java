package ru.haritonenko.commonlibs.dto.error;

public record ErrorMessageResponse(
        String message,
        String detailedMessage,
        String dateTime
) {
}
