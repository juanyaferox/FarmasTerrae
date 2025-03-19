package com.iyg16260.farmasterrae.dto.auth;

import lombok.*;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LoginDTO {
    String username;
    String password;
}
