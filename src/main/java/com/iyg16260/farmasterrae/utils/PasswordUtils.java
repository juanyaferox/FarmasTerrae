package com.iyg16260.farmasterrae.utils;

import java.nio.charset.StandardCharsets;

/** Shared password rules for registration, recovery, changes and bootstrap credentials. */
public final class PasswordUtils {
    private PasswordUtils() { }

    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < 12 || password.length() > 64
                || password.getBytes(StandardCharsets.UTF_8).length > 72) return false;
        boolean upper = false, lower = false, digit = false;
        for (int i = 0; i < password.length(); i++) {
            char c = password.charAt(i);
            if (Character.isISOControl(c)) return false;
            upper |= Character.isUpperCase(c);
            lower |= Character.isLowerCase(c);
            digit |= Character.isDigit(c);
        }
        return upper && lower && digit;
    }
}
