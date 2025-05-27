package com.iyg16260.farmasterrae.dto.user;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.enums.PaymentMethod;
import com.iyg16260.farmasterrae.enums.SaleStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class OrderDetailsDTO {
    Map<ProductDTO, Integer> products;
    BigDecimal totalPrice;
    String status;
    String createdAt;
    String updatedAt;
    String paymentMethod;
    String name;
    String address;

    public void setStatus(SaleStatus saleStatus) {
        this.status = saleStatus.getValue();
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod != null ? paymentMethod.getValue() : null;
    }
}
