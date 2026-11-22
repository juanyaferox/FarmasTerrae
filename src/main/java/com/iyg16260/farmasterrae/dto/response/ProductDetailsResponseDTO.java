package com.iyg16260.farmasterrae.dto.response;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.user.ReviewDTO;

import java.util.List;

/**
 * Detalle de un producto con sus reseñas
 *
 * @param product producto
 * @param reviews reseñas del producto
 */
public record ProductDetailsResponseDTO(ProductDTO product, List<ReviewDTO> reviews) {
}
