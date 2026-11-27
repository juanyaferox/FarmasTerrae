package com.iyg16260.farmasterrae.dto.products;

import com.iyg16260.farmasterrae.enums.Category;
import jakarta.validation.constraints.*;
import jakarta.validation.groups.Default;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ProductDTO {

    /** Validation group applied when creating a new product (enforces URL-safe reference format). */
    public interface OnCreate extends Default {}

    @NotBlank (message = "Debe tener un nombre")
    @Size (max = 255, message = "El nombre no puede superar los 255 caracteres")
    String name;
    @Size (max = 1500, message = "La descripción no puede superar los 1500 caracteres")
    String description;
    String imagePath;
    @NotNull (message = "Debe tener un precio")
    @DecimalMin (value = "0.01", message = "El precio debe ser al menos 0,01 €")
    @DecimalMax (value = "9999999.99", message = "El precio no puede superar 9.999.999,99 €")
    @Digits (integer = 7, fraction = 2, message = "El precio debe tener como máximo 7 cifras enteras y 2 decimales")
    BigDecimal price;
    @NotNull (message = "Debe tener un stock")
    @Min (value = 0, message = "El stock no puede ser negativo")
    @Max (value = 1000000, message = "El stock no puede superar 1.000.000 unidades")
    Integer stock;
    @NotBlank (groups = OnCreate.class, message = "Debe tener una referencia")
    @Size (max = 64, groups = OnCreate.class, message = "La referencia no puede superar los 64 caracteres")
    @Pattern (regexp = "^(?![.]{1,2}$)[A-Za-z0-9._-]+$", groups = OnCreate.class,
            message = "La referencia solo puede contener letras, números, puntos, guiones y guiones bajos")
    String reference;
    @NotNull (message = "La categoría debe ser seleccionada")
    Category category;
}
