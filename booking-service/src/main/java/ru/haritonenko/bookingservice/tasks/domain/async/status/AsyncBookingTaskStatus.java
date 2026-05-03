package ru.haritonenko.bookingservice.tasks.domain.async.status;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import ru.haritonenko.commonlibs.utils.EnumUtils;

public enum AsyncBookingTaskStatus implements EnumUtils.IntEnum, EnumUtils.StringEnum {

    NEW(1, "NEW"),
    IN_PROGRESS(2, "IN_PROGRESS"),
    SUCCEEDED(3, "SUCCEEDED"),
    FAILED_RETRYABLE(4, "FAILED_RETRYABLE"),
    FAILED_NON_RETRYABLE(5, "FAILED_NON_RETRYABLE");

    private final int code;
    private final String value;

    AsyncBookingTaskStatus(int code, String value) {
        this.code = code;
        this.value = value;
    }

    @JsonCreator
    public static AsyncBookingTaskStatus fromStringValue(String value) {
        return EnumUtils.fromValue(AsyncBookingTaskStatus.class, value);
    }

    @Override
    public int getCode() {
        return code;
    }

    @JsonValue
    @Override
    public String getValue() {
        return value;
    }
}
