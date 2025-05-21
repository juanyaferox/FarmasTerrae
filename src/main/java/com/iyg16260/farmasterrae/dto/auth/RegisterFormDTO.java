package com.iyg16260.farmasterrae.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterFormDTO {
    @NotBlank (message = "El usuario no puede estar vacío")
    @Size (max = 50, message = "El título debe tener un máximo de 50 caracteres")
    String username;
    String fullName;
    @NotBlank (message = "El correo no puede estar vacío")
    @Email (message = "Debe ser un correo")
    String email;
    @NotBlank
    @Size (min = 6, max = 20, message = "La contraseña necesita tener 6 caracteres")
    @Pattern (regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).*$",
            message = "La contraseña debe cumplir el patrón")
    String password;
    String confirmPassword;
    String phone;
}
