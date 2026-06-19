package ru.haritonenko.userservice.domain;

import ru.haritonenko.commonlibs.security.authorization.role.PlatformRole;

import java.util.Arrays;
import java.util.List;

public enum UserRole {
    ADMIN(PlatformRole.ADMIN),
    BOOKING_MANAGER(PlatformRole.BOOKING_MANAGER),
    CONTENT_MANAGER(PlatformRole.CONTENT_MANAGER),
    FINANCE_MANAGER(PlatformRole.FINANCE_MANAGER),
    SUPPORT_MANAGER(PlatformRole.SUPPORT_MANAGER),
    USER(PlatformRole.USER);

    private final PlatformRole platformRole;

    UserRole(PlatformRole platformRole) {
        this.platformRole = platformRole;
    }

    public String authority() {
        return platformRole.authority();
    }

    public boolean isStaff() {
        return platformRole.isStaff();
    }

    public static List<UserRole> adminAssignableRoles() {
        return Arrays.stream(values()).toList();
    }
}
