package com.example.demo.common.utils;

public final class SecurityConstants {

    public static final String ROLE_PREFIX = "ROLE_";

    public static final String ROLE_USER = "USER";

    public static final String ROLE_ADMIN = "ADMIN";

    public static final String ROLE_SUPER_ADMIN = "SUPER_ADMIN";

    public static final String HAS_ROLE_ADMIN = "hasRole('ADMIN') or hasRole('SUPER_ADMIN')";

    private SecurityConstants() {
    }
}
