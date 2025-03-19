package com.iyg16260.farmasterrae.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class SaleEnum {

    @Getter
    public enum SaleStatus {
        PENDING("Pendiente"),
        CANCELLED("Cancelado"),
        COMPLETED("Completado");

        private final String value;

        SaleStatus(String value) {
            this.value = value;
        }
    }

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
}

