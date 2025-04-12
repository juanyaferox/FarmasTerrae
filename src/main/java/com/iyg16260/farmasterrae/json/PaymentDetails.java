package com.iyg16260.farmasterrae.json;

import com.iyg16260.farmasterrae.enums.SaleEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class PaymentDetails {
    SaleEnum.PaymentMethod method;
    //String paymentToken;
    //String transactionId;
    LocalDateTime paymentDate;
}
