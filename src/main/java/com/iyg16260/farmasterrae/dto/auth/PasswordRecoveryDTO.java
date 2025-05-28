package com.iyg16260.farmasterrae.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PasswordRecoveryDTO {
    @NotBlank
    @Email
    String email;
    String message;
    String messageType;
    String newPassword;
    String confirmPassword;
}
