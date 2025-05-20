package com.iyg16260.farmasterrae.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserDTO {
    @NotBlank (message = "El usuario no puede estar vacío")
    @Size (max = 50, message = "El título debe tener un máximo de 50 caracteres")
    String username;
    @NotBlank (message = "El nombre no puede estar vacío")
    String fullName;
    @NotBlank (message = "El correo no puede estar vacío")
    @Email (message = "Debe ser un correo")
    String email;
    String phone;
    String address;
}
