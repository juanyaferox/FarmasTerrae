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
        String stringType = type.getValue();
        char genderLetter = stringType.endsWith("a") ? 'a' : 'o';

        switch (operation) {
            case POST:
                return stringType + " añadid" + genderLetter + " con éxito.";
            case DELETE:
                return stringType + " eliminad" + genderLetter + " con éxito.";
            case PUT:
                return stringType + " actualizad" + genderLetter + " con éxito.";
            default:
                return "Operación en " + stringType + " realizad" + genderLetter + " con éxito";
        }
    }
}
