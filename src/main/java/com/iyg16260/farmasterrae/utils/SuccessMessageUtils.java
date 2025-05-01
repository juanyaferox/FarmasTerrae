package com.iyg16260.farmasterrae.utils;

import com.iyg16260.farmasterrae.enums.EntityType;
import com.iyg16260.farmasterrae.enums.Operation;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

public class SuccessMessageUtils {

    private static final String success = "successMessage";

    public static void buildSuccessMessage(RedirectAttributes ra, EntityType type, Operation operation) {
        String message = auxiliarForSuccessMessage(type, operation);
        ra.addFlashAttribute("successMessage", message);
    }

    private static String auxiliarForSuccessMessage(EntityType type, Operation operation) {
        switch (operation) {
            case POST:
                return type.getValue() + " añadido con éxito.";
            case DELETE:
                return type.getValue() + " eliminado con éxito.";
            case PUT:
                return type.getValue() + " actualizado con éxito.";
            default:
                return "Operación en " + type.getValue() + " realizado con éxito";
        }
    }
}
