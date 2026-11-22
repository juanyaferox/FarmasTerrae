package com.iyg16260.farmasterrae.dto.response;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;

import java.util.List;

/**
 * Datos de la página de inicio
 *
 * @param featuredProducts productos destacados
 */
public record HomeResponseDTO(List<ProductDTO> featuredProducts) {
}
