package com.iyg16260.farmasterrae.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterFormDTO {
    @NotBlank(message = "Username is mandatory")
    String username;
    @NotBlank(message = "Email is mandatory")
    @Email
    String email;
    @NotBlank
    @Size(min=8,max=100,message = "The password needs to have at least 8 characters")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).*$",
    message = "The password should contain at least a number, a uppercase, a lowercase and a special character")
    String password;
    String phone;
}
