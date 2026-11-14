package com.example.ecom.common.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;

public final class CookieUtils {

    private CookieUtils() {
    }

    public static void addCookie(HttpServletResponse response, String cookieName, String cookieValue, long validityInSeconds, boolean production) {
        ResponseCookie cookie = ResponseCookie.from(cookieName, cookieValue != null ? cookieValue : "")
                .httpOnly(true)
                .secure(production)
                .path("/")
                .maxAge(validityInSeconds)
                .sameSite(production ? "None" : "Lax")
                .build();

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public static String getCookieValue(HttpServletRequest request, String cookieName) {
        if (request == null || cookieName == null) {
            return null;
        }

        Cookie[] cookies = request.getCookies();

        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (cookieName.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        return null;
    }
}
