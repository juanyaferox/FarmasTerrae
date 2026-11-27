package com.iyg16260.farmasterrae.dto.user;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class OrderDetailsDTO {
    private List<OrderItemDTO> items;
    private BigDecimal totalPrice;
    private String status;
    private String createdAt;
    private String updatedAt;
    private String paymentMethod;
    private String name;
    private String address;
}
