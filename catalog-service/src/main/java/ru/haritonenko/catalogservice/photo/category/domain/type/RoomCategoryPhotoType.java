package ru.haritonenko.catalogservice.photo.category.domain.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import ru.haritonenko.commonlibs.utils.EnumUtils;

public enum RoomCategoryPhotoType implements EnumUtils.IntEnum, EnumUtils.StringEnum {

    MAIN(1, "MAIN"),
    DEFAULT(2, "DEFAULT"),
    GALLERY(3, "GALLERY");

    private final int code;
    private final String value;

    RoomCategoryPhotoType(int code, String value) {
        this.code = code;
        this.value = value;
    }

    @JsonCreator
    public static RoomCategoryPhotoType fromStringValue(String value) {
        return EnumUtils.fromValue(RoomCategoryPhotoType.class, value);
    }

    public static RoomCategoryPhotoType fromCode(int code) {
        return EnumUtils.fromCode(RoomCategoryPhotoType.class, code);
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