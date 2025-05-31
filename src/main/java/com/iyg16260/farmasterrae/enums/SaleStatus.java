package com.iyg16260.farmasterrae.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

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

    public static List<SaleStatus> findByValueContainsIgnoreCase(String keyword) {
        if (keyword == null)
            return List.of();

        return Arrays.stream(SaleStatus.values())
                .filter(status -> status.getValue().toLowerCase().contains(keyword.toLowerCase()))
                .toList();
    }
}
