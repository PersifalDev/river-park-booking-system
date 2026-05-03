package ru.haritonenko.catalogservice.category.domain.type;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import ru.haritonenko.commonlibs.utils.EnumUtils;

public enum RoomType implements EnumUtils.IntEnum, EnumUtils.StringEnum {

    STANDARD(1, "STANDARD"),
    STANDARD_DOUBLE(2, "STANDARD_DOUBLE"),
    STANDARD_PLUS(3, "STANDARD_PLUS"),
    STUDIO(4, "STUDIO"),
    BUSINESS_STUDIO(5, "BUSINESS_STUDIO"),
    ECONOMY(6, "ECONOMY");

    private final int code;
    private final String value;

    RoomType(int code, String value) {
        this.code = code;
        this.value = value;
    }

    @JsonCreator
    public static RoomType fromStringValue(String value) {
        return EnumUtils.fromValue(RoomType.class, value);
    }

    public static RoomType fromCode(int code) {
        return EnumUtils.fromCode(RoomType.class, code);
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
