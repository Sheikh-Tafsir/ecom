package com.example.ecom.common.utils;

public final class CacheConstants {

    private CacheConstants() {
    }

    public static final String CACHE_VERSION = "v1";

    // --- Entity & Business Caches ---
    public static final String CACHE_USER = "user";
    public static final String CACHE_USERS = "users";
    public static final String CACHE_PROFILE = "profile";
    public static final String CACHE_ROLE = "role";
    public static final String CACHE_ROLES = "roles";
    public static final String CACHE_PRODUCTS = "product";
    public static final String CACHE_PRODUCTS_EDIT = "productEdit";
    public static final String CACHE_CATEGORIES = "categories";
    public static final String CACHE_BANNERS = "banners";
    public static final String CACHE_FAQS = "faqs";
    public static final String CACHE_BLOGS = "blogs";

    // --- Rate Limiting (Used for direct Redis keys) ---
    public static final String RATE_LIMIT_PREFIX = "rl:rate:";
    public static final String CONCURRENCY_LIMIT_PREFIX = "rl:concurrency:";
    public static final String GLOBAL_SCOPE = "global";
    public static final String IP_SCOPE_PREFIX = "ip:";
    public static final String USER_SCOPE_PREFIX = "user:";
    public static final String AUTH_SCOPE_PREFIX = "auth:";
    public static final String OTP_SCOPE_PREFIX = "otp:";

    // --- Security & System Caches ---
    public static final String CACHE_IDEMPOTENCY = "idempotency";
    public static final String CACHE_OTPS = "otps";
    public static final String CACHE_SSE_TICKETS = "sseTickets";
    public static final String CACHE_ROTATED_TOKENS = "rotatedTokens";
    public static final String CACHE_REVOKED_ACCESS_TOKENS = "revokedAccessTokens";
}

