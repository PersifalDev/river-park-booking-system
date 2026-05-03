package ru.haritonenko.catalogservice.services.domain.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.RequiredArgsConstructor;
import ru.haritonenko.commonlibs.utils.EnumUtils;

@RequiredArgsConstructor
public enum ServiceItemType implements EnumUtils.IntEnum, EnumUtils.StringEnum {

    ROOM_SERVICE_DELIVERY(1, "ROOM_SERVICE_DELIVERY"),
    CONTACTLESS_CHECK_IN(2, "CONTACTLESS_CHECK_IN"),
    SAUNA_HAMMAM_POOL(3, "SAUNA_HAMMAM_POOL"),
    EMBANKMENT_PARKING(4, "EMBANKMENT_PARKING"),
    CHILDREN_PLAYROOM(5, "CHILDREN_PLAYROOM"),
    TRANSFER_STATION_AIRPORT(6, "TRANSFER_STATION_AIRPORT"),

    GYM(7, "GYM"),
    HOTEL_ADVERTISING(8, "HOTEL_ADVERTISING"),
    LAUNDRY_DRY_CLEANING(9, "LAUNDRY_DRY_CLEANING"),
    PHOTOSHOOT_LOCATION(10, "PHOTOSHOOT_LOCATION"),
    BEAUTY_SALON(11, "BEAUTY_SALON"),
    COMMUNICATION_INTERNET(12, "COMMUNICATION_INTERNET"),

    CHILDREN_TRAMPOLINE_PARK(13, "CHILDREN_TRAMPOLINE_PARK"),
    BICYCLE_RENTAL(14, "BICYCLE_RENTAL"),
    BOAT_EXCURSIONS(15, "BOAT_EXCURSIONS");

    private final int code;
    private final String value;

    @JsonCreator
    public static ServiceItemType fromStringValue(String value) {
        return EnumUtils.fromValue(ServiceItemType.class, value);
    }

    public static ServiceItemType fromCode(int code) {
        return EnumUtils.fromCode(ServiceItemType.class, code);
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