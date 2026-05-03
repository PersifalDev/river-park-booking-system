package ru.haritonenko.commonlibs.utils;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

public final class EnumUtils {

    private EnumUtils(){}

    public interface IntEnum {
        int getCode();
    }

    public interface StringEnum {
        String getValue();
    }

    public static <E extends Enum<E> & IntEnum> E fromCode(
            Class<E> enumClass,
            int code
    ) {
        for (E e : enumClass.getEnumConstants()) {
            if (e.getCode() == code) {
                return e;
            }
        }
        throw new IllegalArgumentException(
                String.format("Unknown code %d for enum %s", code, enumClass.getSimpleName())
        );
    }

    public static <E extends Enum<E> & StringEnum> E fromValue(
            Class<E> enumClass,
            String value
    ) {
        for (E e : enumClass.getEnumConstants()) {
            if (e.getValue().equals(value)) {
                return e;
            }
        }
        throw new IllegalArgumentException(
                String.format("Unknown value '%s' for enum %s", value, enumClass.getSimpleName())
        );
    }

    public static <E extends Enum<E>, V> void checkUniqueValues(
            Class<E> enumClass,
            Function<E, V> extractor
    ) {
        Set<V> seen = new HashSet<>();
        for (E e : enumClass.getEnumConstants()) {
            V val = extractor.apply(e);
            if (!seen.add(val)) {
                throw new IllegalStateException(
                        String.format("Duplicate value '%s' in enum %s", val, enumClass.getSimpleName())
                );
            }
        }
    }

    public static <E extends Enum<E> & IntEnum> void checkUniqueValues(Class<E> enumClass) {
        checkUniqueValues(enumClass, IntEnum::getCode);
    }

    public static <E extends Enum<E> & StringEnum> void checkUniqueStringValues(Class<E> enumClass) {
        checkUniqueValues(enumClass, StringEnum::getValue);
    }
}
