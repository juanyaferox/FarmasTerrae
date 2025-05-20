package com.iyg16260.farmasterrae.dto.products;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductPageDTO {
    String name;
    String imagePath;
    BigDecimal price;
    String reference;
}
