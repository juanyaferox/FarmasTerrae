package com.iyg16260.farmasterrae.dto.auth;

import com.iyg16260.farmasterrae.utils.PasswordUtils;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

@Data
public class RegisterFormDTO {
    @NotBlank(message = "El usuario no puede estar vacío")
    @Size(min = 3, max = 20, message = "El usuario debe tener entre 3 y 20 caracteres")
    @Pattern(regexp = "^[a-zA-Z0-9_]{3,20}$", message = "El usuario solo puede contener letras, números y guion bajo")
    private String username;
    @Size(max = 255)
    private String fullName;
    @NotBlank(message = "El correo no puede estar vacío") @Email(message = "Debe ser un correo") @Size(max = 254)
    private String email;
    @NotBlank @Size(min = 12, max = 64) @ToString.Exclude
    private String password;
    @NotBlank @ToString.Exclude private String confirmPassword;
    @Size(max = 255)
    private String phone;

    @AssertTrue(message = "La contraseña debe tener entre 12 y 64 caracteres, mayúsculas, minúsculas y números")
    public boolean isPasswordPolicyValid() { return password == null || password.isBlank() || PasswordUtils.isValidPassword(password); }
    @AssertTrue(message = "No coinciden las contraseñas")
    public boolean isConfirmationValid() { return confirmPassword == null || confirmPassword.isBlank() || (password != null && password.equals(confirmPassword)); }
}
