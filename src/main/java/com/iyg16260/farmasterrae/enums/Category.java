package com.iyg16260.farmasterrae.enums;

import lombok.Getter;

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
}
