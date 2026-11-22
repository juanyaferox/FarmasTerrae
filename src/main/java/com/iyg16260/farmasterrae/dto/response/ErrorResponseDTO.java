package com.iyg16260.farmasterrae.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta de error común para toda la API
 *
 * @param timestamp momento del error
 * @param status    código HTTP
 * @param error     descripción del código HTTP
 * @param message   mensaje del error
 * @param errors    listado de errores de validación (solo si los hay)
 * @param errorId   identificador del error registrado en el log (solo si se ha registrado)
 */
@JsonInclude (JsonInclude.Include.NON_NULL)
public record ErrorResponseDTO(LocalDateTime timestamp,
                               int status,
                               String error,
                               String message,
                               List<String> errors,
                               String errorId) {

    public static ErrorResponseDTO of(HttpStatusCode status, String message) {
        return of(status, message, null, null);
    }

    public static ErrorResponseDTO of(HttpStatusCode status, String message, List<String> errors, String errorId) {
        HttpStatus httpStatus = HttpStatus.resolve(status.value());
        return new ErrorResponseDTO(
                LocalDateTime.now(),
                status.value(),
                httpStatus != null ? httpStatus.getReasonPhrase() : null,
                message,
                errors,
                errorId);
    }
}
