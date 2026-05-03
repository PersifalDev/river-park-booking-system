package ru.haritonenko.bookingservice.domain.status;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import ru.haritonenko.commonlibs.utils.EnumUtils;

public enum BookingStatus implements EnumUtils.IntEnum, EnumUtils.StringEnum{

    CREATED(1, "CREATED"),
    HOLD(2, "HOLD"),
    CONFIRMED(3, "CONFIRMED"),
    CANCELLED(4, "CANCELLED"),
    FAILED(5, "FAILED"),
    EXPIRED(6, "EXPIRED");

    private final int code;
    private final String value;

    BookingStatus(int code, String value) {
        this.code = code;
        this.value = value;
    }

    @JsonCreator
    public static BookingStatus fromStringValue(String value) {
        return EnumUtils.fromValue(BookingStatus.class, value);
    }

    public static BookingStatus fromCode(int code) {
        return EnumUtils.fromCode(BookingStatus.class, code);
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
