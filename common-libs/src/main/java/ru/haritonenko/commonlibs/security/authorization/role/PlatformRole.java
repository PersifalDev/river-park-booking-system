package ru.haritonenko.commonlibs.security.authorization.role;

import java.util.Set;

public enum PlatformRole {
    ADMIN,
    BOOKING_MANAGER,
    CONTENT_MANAGER,
    FINANCE_MANAGER,
    SUPPORT_MANAGER,
    USER;

    public static final String ADMIN_AUTHORITY = "ADMIN";
    public static final String BOOKING_MANAGER_AUTHORITY = "BOOKING_MANAGER";
    public static final String CONTENT_MANAGER_AUTHORITY = "CONTENT_MANAGER";
    public static final String FINANCE_MANAGER_AUTHORITY = "FINANCE_MANAGER";
    public static final String SUPPORT_MANAGER_AUTHORITY = "SUPPORT_MANAGER";
    public static final String USER_AUTHORITY = "USER";

    private static final Set<PlatformRole> STAFF_ROLES = Set.of(
            ADMIN,
            BOOKING_MANAGER,
            CONTENT_MANAGER,
            FINANCE_MANAGER,
            SUPPORT_MANAGER
    );

    public String authority() {
        return name();
    }

    public boolean isStaff() {
        return STAFF_ROLES.contains(this);
    }
}
