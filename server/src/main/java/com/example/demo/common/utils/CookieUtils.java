package com.example.demo.common.utils;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public final class CookieUtils {

    private CookieUtils() {
    }

    public static void addCookie(HttpServletResponse response, String cookieName, String cookieValue, long validityInSeconds, boolean production) {
        Cookie cookie = new Cookie(cookieName, cookieValue);
        cookie.setMaxAge((int) validityInSeconds);
        cookie.setPath("/");
        cookie.setHttpOnly(true);

        if (production) {
            cookie.setSecure(true);
            cookie.setAttribute("SameSite", "None");
        } else {
            cookie.setSecure(false);
            cookie.setAttribute("SameSite", "Lax");
        }

        response.addCookie(cookie);
    }

    public static String getCookieValue(HttpServletRequest request, String cookieName) {
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
