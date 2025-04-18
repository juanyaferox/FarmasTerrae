package com.iyg16260.farmasterrae.dto.order;

import com.iyg16260.farmasterrae.enums.SaleEnum;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderDTO {
    long id;
    String createdAt;
    Float totalPrice;
    String status;
}
