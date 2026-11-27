package com.iyg16260.farmasterrae.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Datos que un usuario puede cambiar de su propia cuenta desde el panel ({@code /user/dashboard/info-user}).
 * <p>
 * No tiene {@code type}/{@code profile}, {@code id}, {@code password} ni {@code deletedAt}: Spring descarta los
 * parámetros que no corresponden a ninguna propiedad, así que la petición no puede cambiar el rol ni el estado
 * de la cuenta. Un campo opcional que no llega ({@code null}) conserva el valor guardado; si llega vacío, se borra.
 */
@Data
public class SelfProfileUpdateDTO {
    @NotBlank (message = "El usuario no puede estar vacío")
    @Size (min = 3, max = 20, message = "El usuario debe tener entre 3 y 20 caracteres")
    @Pattern (regexp = "^[a-zA-Z0-9_]{3,20}$", message = "El usuario solo puede contener letras, números y guion bajo")
    private String username;

    @Size (max = 255, message = "El nombre completo no puede superar los 255 caracteres")
    private String fullName;

    @NotBlank (message = "El correo no puede estar vacío")
    @Email (message = "Debe ser un correo")
    @Size (max = 254, message = "El correo no puede superar los 254 caracteres")
    private String email;

    @Size (max = 30, message = "El teléfono no puede superar los 30 caracteres")
    private String phone;

    @Size (max = 255, message = "La dirección no puede superar los 255 caracteres")
    private String address;
}
