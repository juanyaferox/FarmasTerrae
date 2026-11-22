package com.iyg16260.farmasterrae.dto.response;

/**
 * Token CSRF que debe enviarse en las peticiones que modifican datos
 *
 * @param headerName    cabecera en la que enviar el token
 * @param parameterName parámetro en el que enviar el token (alternativa a la cabecera)
 * @param token         valor del token
 */
public record CsrfTokenDTO(String headerName, String parameterName, String token) {
}
