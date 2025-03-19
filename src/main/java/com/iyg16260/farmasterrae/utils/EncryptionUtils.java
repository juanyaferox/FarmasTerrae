package com.iyg16260.farmasterrae.utils;

import de.mkammerer.argon2.Argon2;
import de.mkammerer.argon2.Argon2Factory;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

public class EncryptionUtils {

    /**
     *
     * @param password Contraseña a encriptar
     * @return Contraseña encriptada
     */
    public static String hashPassword(String password) {
        Argon2 argon2 = Argon2Factory.create();
        try {
            return argon2.hash(10, 65536, 1, password.toCharArray());
        } finally {
            argon2.wipeArray(password.toCharArray());
        }
    }

    /**
     *
     * @param hashedPassword Contraseña encriptada
     * @param password Contraseña a verificar
     * @return true si coincide, false en caso contrario
     */
    public static boolean verifyPassword(String hashedPassword, String password) {
        Argon2 argon2 = Argon2Factory.create();
        try {
            return argon2.verify(hashedPassword, password.toCharArray());
        } finally {
            argon2.wipeArray(password.toCharArray());
        }
    }

}
