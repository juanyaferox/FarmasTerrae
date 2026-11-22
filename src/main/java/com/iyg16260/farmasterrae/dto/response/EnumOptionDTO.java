package com.iyg16260.farmasterrae.dto.response;

/**
 * Representa un valor de un enum: código (nombre del enum) y etiqueta legible
 *
 * @param code  nombre del enum, el que se debe enviar a la API
 * @param label texto descriptivo
 */
public record EnumOptionDTO(String code, String label) {
}
