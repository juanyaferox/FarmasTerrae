package com.iyg16260.farmasterrae.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Getter
public enum Category {
    MEDICINE("Medicamentos"),
    VITAMIN("Vitaminas y Suplementos"),
    CARE("Cuidado Personal"),
    FIRST_AID("Primeros Auxilios"),
    BEAUTY("Belleza y Cuidado de la Piel"),
    BABY("Productos para Bebés");

    private final String value;

    Category(String value) {
        this.value = value;
    }

    /**
     * Valores cuyo texto contiene {@code keyword} sin distinguir mayúsculas. Una búsqueda nula o en blanco no
     * coincide con ninguno (si no, un espacio coincidiría con todos los textos de varias palabras).
     */
    public static List<Category> findByValueContainsIgnoreCase(String keyword) {
        if (keyword == null || keyword.isBlank())
            return List.of();

        String normalized = keyword.trim().toLowerCase(Locale.ROOT);
        return Arrays.stream(Category.values())
                .filter(category -> category.getValue().toLowerCase(Locale.ROOT).contains(normalized))
                .toList();
    }
}
