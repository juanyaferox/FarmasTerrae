package com.iyg16260.farmasterrae.dto.user;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UpdateReviewDTO {
    @NotNull (message = "El identificador de la reseña es obligatorio")
    Long id;
    @NotBlank (message = "El título no debe de estar vacío")
    @Size (max = 50, message = "El título debe tener un máximo de 50 caracteres")
    String title;
    @NotBlank (message = "El contenido no debe de estar vacío")
    @Size (max = 1000, message = "El contenido debe tener un máximo de 1000 caracteres")
    String content;
    @NotNull (message = "La puntuación es obligatoria")
    @Min (value = 1, message = "La puntuación debe ser al menos 1")
    @Max (value = 10, message = "La puntuación no puede superar 10")
    Integer scoring;
}
