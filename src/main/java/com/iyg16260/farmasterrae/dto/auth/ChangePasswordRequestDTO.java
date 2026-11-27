package com.iyg16260.farmasterrae.dto.auth;

import com.iyg16260.farmasterrae.utils.PasswordUtils;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;

@Data
public class ChangePasswordRequestDTO {
    @NotBlank @ToString.Exclude private String currentPassword;
    @NotBlank @Size(min = 12, max = 64) @ToString.Exclude private String newPassword;
    @NotBlank @ToString.Exclude private String confirmPassword;
    private String message;
    private String messageType;

    @AssertTrue(message = "La contraseña debe tener entre 12 y 64 caracteres, mayúsculas, minúsculas y números")
    public boolean isPasswordPolicyValid() { return newPassword == null || newPassword.isBlank() || PasswordUtils.isValidPassword(newPassword); }
    @AssertTrue(message = "No coinciden las contraseñas")
    public boolean isConfirmationValid() { return confirmPassword == null || confirmPassword.isBlank() || (newPassword != null && newPassword.equals(confirmPassword)); }
}
