package com.iyg16260.farmasterrae.dto.auth;

import lombok.Data;

@Data
public class PasswordRecoveryDTO {
    String email;
    String message;
    String messageType;
}
