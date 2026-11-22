package com.iyg16260.farmasterrae.dto.auth;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty (access = JsonProperty.Access.WRITE_ONLY)
    String newPassword;
    @JsonProperty (access = JsonProperty.Access.WRITE_ONLY)
    String confirmPassword;
}
