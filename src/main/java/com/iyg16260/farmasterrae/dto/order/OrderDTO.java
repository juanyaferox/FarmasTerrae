package com.iyg16260.farmasterrae.dto.order;

import com.iyg16260.farmasterrae.enums.SaleStatus;
import lombok.Data;

@Data
public class OrderDTO {
    long id;
    String createdAt;
    String updatedAt;
    Float totalPrice;
    SaleStatus status;
    String name;
    String address;
}
