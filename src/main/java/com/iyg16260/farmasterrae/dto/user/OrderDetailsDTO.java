package com.iyg16260.farmasterrae.dto.user;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.products.ProductQuantityDTO;
import com.iyg16260.farmasterrae.dto.serializer.ProductQuantityMapSerializer;
import com.iyg16260.farmasterrae.enums.PaymentMethod;
import com.iyg16260.farmasterrae.enums.SaleStatus;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class OrderDetailsDTO {
    @JsonSerialize (using = ProductQuantityMapSerializer.class)
    @ArraySchema (schema = @Schema (implementation = ProductQuantityDTO.class)) // se serializa como array
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
