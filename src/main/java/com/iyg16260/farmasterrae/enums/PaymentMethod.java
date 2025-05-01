package com.iyg16260.farmasterrae.enums;

import lombok.Getter;

@Getter
public enum PaymentMethod {
    CREDIT_CARD("Tarjeta de crédito"),
    PAYPAL("Paypal"),
    TRANSFER("Transferencia bancaria");

    private final String value;

    PaymentMethod(String value) {
        this.value = value;
    }
}
