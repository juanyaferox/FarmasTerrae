 package com.iyg16260.farmasterrae.validation;

 import org.springframework.stereotype.Component;
 import lombok.extern.slf4j.Slf4j;
 import java.lang.reflect.Field;
 import java.util.Arrays;
 import java.util.ResourceBundle;

 @Component
 @Slf4j
 public class ObjectValidator {

     public static boolean isValid(Object obj) {
         if (obj == null) {
             return false;
         }

         return Arrays.stream(obj.getClass().getDeclaredFields())
                 .filter(field -> !"id".equalsIgnoreCase(field.getName())) // Ignorar campo "id"
                 .allMatch(field -> {
                     try {
                         field.setAccessible(true); // Permite acceder a los campos privados
                         return field.get(obj) != null;
                     } catch (IllegalAccessException e) {
                         return false; // Si hay un error al acceder, consideramos el campo como inválido
                     }
                 });
     }
 }


