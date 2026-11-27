package com.iyg16260.farmasterrae.dto.order;

import com.iyg16260.farmasterrae.enums.SaleStatus;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderDTO {
    private Long id;
    private String createdAt;
    private String updatedAt;
    private BigDecimal totalPrice;
    private SaleStatus status;
    private String name;
    private String address;
}
