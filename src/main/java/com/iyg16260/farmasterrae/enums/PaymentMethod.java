package com.iyg16260.farmasterrae.enums;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

@Getter
public enum PaymentMethod {
    CREDIT_CARD("Tarjeta de crédito"),
    PAYPAL("Paypal"),
    TRANSFER("Transferencia bancaria");

    private final String value;

    PaymentMethod(String value) {
        this.value = value;
    }

    public static List<PaymentMethod> findByValueContainsIgnoreCase(String keyword) {
        if (keyword == null)
            return List.of();

        return Arrays.stream(PaymentMethod.values())
                .filter(method -> method.getValue().toLowerCase().contains(keyword.toLowerCase()))
                .toList();
    }
}
