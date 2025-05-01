package com.iyg16260.farmasterrae.enums;

public enum EntityType {
    PRODUCTS("Producto"),
    ORDERS("Pedido"),
    USERS("Usuario");

    private final String value;

    EntityType(String value) {
        this.value = value;
    }

    public String get() {
        return value;
    }
}
