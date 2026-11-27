package com.iyg16260.farmasterrae.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Formulario de edición de usuarios del panel de administración ({@code PUT /admin/dashboard/users}).
 * <p>
 * {@code type} solo admite los perfiles que conoce {@code SecurityConfig}; además, {@code UserService} comprueba
 * que el perfil exista y que la cuenta no sea el último administrador antes de cambiarlo.
 */
@Data
public class AdminUserUpdateDTO {
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

    @NotBlank (message = "El usuario debe estar asignado a un tipo")
    @Pattern (regexp = "^(ADMIN|USER)$", message = "Tipo de usuario inválido")
    private String type;
}
