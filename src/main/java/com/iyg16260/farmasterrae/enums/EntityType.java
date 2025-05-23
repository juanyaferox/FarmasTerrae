package com.iyg16260.farmasterrae.enums;

import lombok.Getter;

@Getter
public enum EntityType {
    PRODUCTS("Producto"),
    ORDERS("Pedido"),
    USERS("Usuario"),
    REVIEWS("Reseña");

    private final String value;

    EntityType(String value) {
        this.value = value;
    }
}
