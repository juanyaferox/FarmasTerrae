package com.iyg16260.farmasterrae.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Getter
public enum PaymentMethod {
    CREDIT_CARD("Tarjeta de crédito"),
    PAYPAL("Paypal"),
    TRANSFER("Transferencia bancaria");

    private final String value;

    PaymentMethod(String value) {
        this.value = value;
    }

    /**
     * Valores cuyo texto contiene {@code keyword} sin distinguir mayúsculas. Una búsqueda nula o en blanco no
     * coincide con ninguno (si no, un espacio coincidiría con todos los textos de varias palabras).
     */
    public static List<PaymentMethod> findByValueContainsIgnoreCase(String keyword) {
        if (keyword == null || keyword.isBlank())
            return List.of();

        String normalized = keyword.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(PaymentMethod.values())
                .filter(method -> method.getValue().toLowerCase(Locale.ROOT).contains(normalized))
                .toList();
    }
}
