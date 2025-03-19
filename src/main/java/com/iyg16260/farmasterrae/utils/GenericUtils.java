package com.iyg16260.farmasterrae.utils;

public class GenericUtils {
    /**
     * Comprueba si un DNI es válido
     * @param dni DNI a comprobar
     * @return true si es válido, false en caso contrario
     */
    @SuppressWarnings("unused")
    private static boolean isDNIValid(String dni) {
        if (dni == null || !dni.matches("\\d{8}[A-Z]")) {
            return false;
        }
        int numeroDNI = Integer.parseInt(dni.substring(0, 8));
        char letraDNI = dni.charAt(8);
        String letras_DNI = "TRWAGMYFPDXBNJZSQVHLCKE";
        char letraCorrecta = letras_DNI.charAt(numeroDNI % 23);

        return letraDNI == letraCorrecta;
    }
}
