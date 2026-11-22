package com.iyg16260.farmasterrae.dto.products;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.web.multipart.MultipartFile;

/**
 * Solo se usa para documentar en OpenAPI el formulario multipart/form-data de alta y edición de productos:
 * los campos de {@link ProductDTO} más la imagen
 */
@Schema (name = "ProductForm",
        description = "Campos del producto enviados como multipart/form-data, con la imagen opcional")
public class ProductFormDTO extends ProductDTO {

    @Schema (type = "string", format = "binary", description = "Imagen del producto (opcional)")
    private MultipartFile imageFile;

    public MultipartFile getImageFile() {
        return imageFile;
    }
}
