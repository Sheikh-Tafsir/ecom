package com.example.demo.common.utils;

public final class SecurityConstants {

    public static final String ROLE_PREFIX = "ROLE_";

    public static final String ROLE_ADMIN = "ADMIN";

    public static final String HAS_ROLE_ADMIN = "hasRole('ADMIN')";

    public static final String NOT_ADMIN_OR_OWNER_EXCEPTION = "User is not Admin or is Owner of this Entity";

    private SecurityConstants() {
    }
}
