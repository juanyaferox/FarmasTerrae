package com.iyg16260.farmasterrae.enums;

import lombok.Getter;

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
}
