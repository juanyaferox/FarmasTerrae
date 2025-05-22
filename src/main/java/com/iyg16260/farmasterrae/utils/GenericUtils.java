package com.iyg16260.farmasterrae.utils;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

public class GenericUtils {
    /**
     * Comprueba si un DNI es válido
     *
     * @param dni DNI a comprobar
     * @return true si es válido, false en caso contrario
     */
    @SuppressWarnings ("unused")
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

    public static BigDecimal priceAmountCalc(Map<ProductDTO, Integer> cart) {
        return cart.entrySet().stream()
                .map(entry -> entry.getKey().getPrice()
                        .multiply(BigDecimal.valueOf(entry.getValue())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
    }


}
