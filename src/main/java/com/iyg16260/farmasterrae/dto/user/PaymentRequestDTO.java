package com.iyg16260.farmasterrae.dto.user;

import lombok.Data;

@Data
public class PaymentRequestDTO {
    private String cardNumber;
    private String cardHolder;
    private Double amount;
}
