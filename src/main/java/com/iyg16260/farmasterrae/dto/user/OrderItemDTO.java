package com.iyg16260.farmasterrae.dto.user;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {
    private String productReference;
    private String productName;
    private String imagePath;
    private BigDecimal unitPrice;
    private Integer amount;
    private BigDecimal subtotal;
}
