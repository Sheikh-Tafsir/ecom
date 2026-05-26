package com.example.demo.common.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

public final class CookieUtils {

    private CookieUtils() {
        throw new AssertionError("No instances.");
    }

    public static void addCookie(HttpServletResponse response, String cookieName, String cookieValue, long validityInSeconds) {
        Cookie cookie = new Cookie(cookieName, cookieValue);
        cookie.setMaxAge((int) validityInSeconds);
        cookie.setPath("/");
        cookie.setSecure(true);
        cookie.setHttpOnly(true);
        cookie.setAttribute("SameSite", "Strict"); // or "Lax"
        response.addCookie(cookie);
    }
}
