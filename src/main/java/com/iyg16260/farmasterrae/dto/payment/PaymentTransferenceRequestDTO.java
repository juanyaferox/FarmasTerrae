package com.iyg16260.farmasterrae.dto.payment;

import lombok.Data;

@Data
public class PaymentTransferenceRequestDTO {
    private String fullname;
    private String iban;
    private String reference;
}
