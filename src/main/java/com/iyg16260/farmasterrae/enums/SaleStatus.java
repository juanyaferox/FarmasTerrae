package com.iyg16260.farmasterrae.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Getter
public enum SaleStatus {
    PENDING("Pendiente"),
    CANCELLED("Cancelado"),
    COMPLETED("Completado"),
    SHIPPED("Enviado");

    private final String value;

    SaleStatus(String value) {
        this.value = value;
    }

    public boolean canTransitionTo(SaleStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case PENDING -> target == SHIPPED || target == COMPLETED || target == CANCELLED;
            case SHIPPED -> target == COMPLETED || target == CANCELLED;
            case COMPLETED, CANCELLED -> false;
        };
    }

    /**
     * Valores cuyo texto contiene {@code keyword} sin distinguir mayúsculas. Una búsqueda nula o en blanco no
     * coincide con ninguno (si no, un espacio coincidiría con todos los textos de varias palabras).
     */
    public static List<SaleStatus> findByValueContainsIgnoreCase(String keyword) {
        if (keyword == null || keyword.isBlank())
            return List.of();

        String normalized = keyword.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(SaleStatus.values())
                .filter(status -> status.getValue().toLowerCase(Locale.ROOT).contains(normalized))
                .toList();
    }
}
