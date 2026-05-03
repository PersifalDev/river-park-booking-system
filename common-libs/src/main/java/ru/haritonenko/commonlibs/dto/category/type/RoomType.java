package ru.haritonenko.commonlibs.dto.category.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum RoomType {
    STANDARD,
    STANDARD_DOUBLE,
    STANDARD_PLUS,
    STUDIO,
    BUSINESS_STUDIO,
    ECONOMY;

    @JsonCreator
    public static RoomType fromValue(String value) {
        return value == null ? null : RoomType.valueOf(value.toUpperCase());
    }

    @JsonValue
    public String getValue() {
        return name();
    }
}
