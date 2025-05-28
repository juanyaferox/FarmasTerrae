package com.iyg16260.farmasterrae.dto.payment;

import com.iyg16260.farmasterrae.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class PaymentDetailsDTO {
    PaymentMethod paymentMethod;
    BigDecimal amount;
    @NotBlank (message = "Debe tener una dirección")
    String address;
    @NotBlank (message = "Debe tener un nombre")
    String fullName;
}
