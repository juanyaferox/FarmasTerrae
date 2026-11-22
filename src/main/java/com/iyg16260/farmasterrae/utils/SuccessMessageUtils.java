package com.iyg16260.farmasterrae.utils;

import com.iyg16260.farmasterrae.dto.response.MessageResponseDTO;
import com.iyg16260.farmasterrae.enums.EntityType;
import com.iyg16260.farmasterrae.enums.Operation;

public class SuccessMessageUtils {

    /**
     * Construye el mensaje de éxito de la operación
     *
     * @param type      tipo de entidad
     * @param operation operación realizada
     * @return respuesta con el mensaje de éxito
     */
    public static MessageResponseDTO buildSuccessMessage(EntityType type, Operation operation) {
        return new MessageResponseDTO(auxiliarForSuccessMessage(type, operation));
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
