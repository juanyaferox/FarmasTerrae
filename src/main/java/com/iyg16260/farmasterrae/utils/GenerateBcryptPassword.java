package com.iyg16260.farmasterrae.utils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class GenerateBcryptPassword {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String rawPassword = "123"; // Cambia "tuContraseña" por la contraseña deseada
        String encodedPassword = encoder.encode(rawPassword);
        System.out.println(encodedPassword);
    }
}