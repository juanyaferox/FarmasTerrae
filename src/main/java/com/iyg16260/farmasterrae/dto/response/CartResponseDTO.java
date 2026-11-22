package com.iyg16260.farmasterrae.dto.response;

import com.iyg16260.farmasterrae.dto.products.ProductQuantityDTO;

import java.math.BigDecimal;
import java.util.List;

/**
 * Contenido del carrito
 *
 * @param products    productos con su cantidad
 * @param totalAmount precio total
 */
public record CartResponseDTO(List<ProductQuantityDTO> products, BigDecimal totalAmount) {
}
