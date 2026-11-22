package com.iyg16260.farmasterrae.dto.response;

/**
 * Respuesta tras crear un pedido
 *
 * @param id      id del pedido creado
 * @param message mensaje de la operación
 */
public record OrderCreatedResponseDTO(long id, String message) {
}
