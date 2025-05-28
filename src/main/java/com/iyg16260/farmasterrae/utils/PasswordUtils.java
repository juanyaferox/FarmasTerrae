package com.iyg16260.farmasterrae.utils;

public class PasswordUtils {

    /**
     * Valida que la contraseña cumpla:
     * - Al menos una mayúscula
     * - Al menos un dígito
     * - Longitud entre 6 y 20 caracteres
     * - Solo letras (mayúsculas o minúsculas) y dígitos
     *
     * @param password la contraseña a validar
     * @return true si cumple el patrón, false en caso contrario
     */
    public static boolean isValidPassword(String password) {
        if (password == null) {
            return false;
        }
        // Regex:
        // (?=.*[A-Z])   → al menos una mayúscula
        // (?=.*\d)      → al menos un dígito
        // [A-Za-z\d]    → solo letras y dígitos
        // {6,20}        → entre 6 y 20 caracteres de longitud
        String pattern = "^(?=.*[A-Z])(?=.*\\d)[A-Za-z\\d]{6,20}$";
        return password.matches(pattern);
    }
}

