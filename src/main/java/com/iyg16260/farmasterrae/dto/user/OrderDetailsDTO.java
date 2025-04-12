package com.iyg16260.farmasterrae.dto.user;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import lombok.Data;

import java.util.Map;

@Data
public class OrderDetailsDTO {
    Map <ProductDTO,Integer> products;
    Double totalPrice;
    String status;
    String createdAt;
    String updatedAt;
    String paymentMethod;
}
