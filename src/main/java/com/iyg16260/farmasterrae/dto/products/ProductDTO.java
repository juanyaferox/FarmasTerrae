package com.iyg16260.farmasterrae.dto.products;

import com.iyg16260.farmasterrae.enums.Category;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductDTO {
    @NotBlank (message = "Debe tener un nombre")
    String name;
    String description;
    String imagePath;
    @NotNull (message = "Debe tener un precio")
    BigDecimal price;
    Integer stock;
    @NotBlank (message = "Debe tener una referencia")
    String reference;
    @NotNull (message = "La categoría debe ser seleccionada")
    Category category;
}
