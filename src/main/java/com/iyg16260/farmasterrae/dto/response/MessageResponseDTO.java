package com.iyg16260.farmasterrae.dto.response;

/**
 * Respuesta genérica con un mensaje informativo (sustituye a los flash attributes "successMessage")
 *
 * @param message mensaje de la operación
 */
public record MessageResponseDTO(String message) {
}
