package com.iyg16260.farmasterrae.dto.payment;

import com.iyg16260.farmasterrae.enums.PaymentMethod;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class PaymentDetailsDTO {
    PaymentMethod paymentMethod;
    BigDecimal amount;
    String address;
    String full_name;
}
