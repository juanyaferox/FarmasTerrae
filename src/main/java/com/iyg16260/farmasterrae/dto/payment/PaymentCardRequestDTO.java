package com.iyg16260.farmasterrae.dto.payment;

import lombok.Data;

@Data
public class PaymentCardRequestDTO {
    private String cardNumber;
    private String cardCVC;
    private String cardHolder;
    private String endDate;
}
