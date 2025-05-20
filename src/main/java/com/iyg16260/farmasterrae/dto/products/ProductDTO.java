package com.iyg16260.farmasterrae.dto.products;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductDTO {
    String name;
    String description;
    String imagePath;
    BigDecimal price;
    Integer stock;
    String reference;
}
