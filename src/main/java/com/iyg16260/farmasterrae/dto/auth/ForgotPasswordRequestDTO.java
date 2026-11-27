package com.iyg16260.farmasterrae.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ForgotPasswordRequestDTO {
    @NotBlank @Email @Size(max = 254)
    private String email;
    private String message;
    private String messageType;
}
