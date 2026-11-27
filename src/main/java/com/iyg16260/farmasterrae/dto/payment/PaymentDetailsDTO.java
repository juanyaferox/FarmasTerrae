package com.iyg16260.farmasterrae.dto.payment;

import com.iyg16260.farmasterrae.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PaymentDetailsDTO {
    @NotNull (message = "Debe seleccionar un método de pago")
    private PaymentMethod paymentMethod;
    @NotBlank (message = "Debe tener una dirección")
    private String address;
    @NotBlank (message = "Debe tener un nombre")
    private String fullName;
}
