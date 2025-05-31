package com.iyg16260.farmasterrae.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

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

    public static List<Category> findByValueContainsIgnoreCase(String keyword) {
        if (keyword == null)
            return List.of();

        return Arrays.stream(Category.values())
                .filter(category -> category.getValue().toLowerCase().contains(keyword.toLowerCase()))
                .toList();
    }
}
