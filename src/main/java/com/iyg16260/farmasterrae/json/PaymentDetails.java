package com.iyg16260.farmasterrae.json;

import com.iyg16260.farmasterrae.enums.SaleEnum;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PaymentDetails {
    String method;
    //String paymentToken;
    //String transactionId;
    LocalDateTime paymentDate;

    public PaymentDetails (SaleEnum.PaymentMethod method, LocalDateTime paymentDate) {
        this.method = method.getValue();
        this.paymentDate = paymentDate;
    }

    public void setMethod (SaleEnum.PaymentMethod method) {
        this.method = method.getValue();
    }
}
