package ru.haritonenko.bookingservice.tasks.domain.async.status;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import ru.haritonenko.commonlibs.utils.EnumUtils;

public enum ProcessingStep implements EnumUtils.IntEnum, EnumUtils.StringEnum {

    VALIDATE_REQUEST(1, "VALIDATE_REQUEST"),
    CHECK_AVAILABILITY(2, "CHECK_AVAILABILITY"),
    CALCULATE_PRICE(3, "CALCULATE_PRICE"),
    CREATE_HOLD(4, "CREATE_HOLD"),
    SAVE_BOOKING(5, "SAVE_BOOKING"),
    CONFIRM_BOOKING(6, "CONFIRM_BOOKING"),
    CANCEL_BOOKING(7, "CANCEL_BOOKING"),
    EXPIRE_HOLD(8, "EXPIRE_HOLD"),
    RELEASE_INVENTORY(9, "RELEASE_INVENTORY");;

    private final int code;
    private final String value;

    ProcessingStep(int code, String value) {
        this.code = code;
        this.value = value;
    }

    @JsonCreator
    public static ProcessingStep fromStringValue(String value) {
        return EnumUtils.fromValue(ProcessingStep.class, value);
    }

    public static ProcessingStep fromCode(int code) {
        return EnumUtils.fromCode(ProcessingStep.class, code);
    }

    @JsonValue
    @Override
    public String getValue() {
        return value;
    }

    @Override
    public int getCode() {
        return code;
    }
}
