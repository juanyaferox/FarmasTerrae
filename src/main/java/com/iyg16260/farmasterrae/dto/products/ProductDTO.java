package com.iyg16260.farmasterrae.dto.products;

import lombok.Data;

@Data
public class ProductDTO {
    String name;
    String description;
    String imagePath;
    Double price;
    Integer stock;
    String reference;
}
