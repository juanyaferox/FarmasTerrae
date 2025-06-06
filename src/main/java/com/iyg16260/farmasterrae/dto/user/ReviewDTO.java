package com.iyg16260.farmasterrae.dto.user;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class ReviewDTO {
    Long id;
    @Size (max = 50, message = "El título debe tener un máximo de 50 caracteres")
    @NotBlank (message = "El título no debe de estar vacío")
    String title;
    @Size (max = 1000)
    @NotBlank (message = "El contenido no debe de estar vacío")
    String content;
    @Max (10)
    @Min (1)
    @NotNull (message = "La puntuación es obligatoria")
    int scoring;
    String productReference;
    String productImage;
    String productName;
}
