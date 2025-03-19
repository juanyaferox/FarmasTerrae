package com.iyg16260.farmasterrae.validation;

public class PasswordValidator {

    private static final int MIN_LENGTH = 8;
    private static final String SPECIAL_CHARS = "!@#$%^&*()-+=<>?";

    public static boolean isValidPassword(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            return false;
        }

        boolean hasUpperCase = !password.equals(password.toLowerCase());
        boolean hasLowerCase = !password.equals(password.toUpperCase());
        boolean hasDigit = password.chars().anyMatch(Character::isDigit);
        boolean hasSpecialChar = password.chars().anyMatch(ch -> SPECIAL_CHARS.indexOf(ch) >= 0);

        return hasUpperCase && hasLowerCase && hasDigit && hasSpecialChar;
    }
}
