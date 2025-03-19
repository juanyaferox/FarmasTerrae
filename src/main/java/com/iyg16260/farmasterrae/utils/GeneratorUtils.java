package com.iyg16260.farmasterrae.utils;

import java.util.Random;

public class GeneratorUtils {

        private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        private static final int CODE_LENGTH = 10;

        public static String generateRandomCode() {
            Random random = new Random();
            StringBuilder code = new StringBuilder(CODE_LENGTH);

            for (int i = 0; i < CODE_LENGTH; i++) {
                int index = random.nextInt(CHARACTERS.length());
                code.append(CHARACTERS.charAt(index));
            }

            return code.toString();
        }
}
