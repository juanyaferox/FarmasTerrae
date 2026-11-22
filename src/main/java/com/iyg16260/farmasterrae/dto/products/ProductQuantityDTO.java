package com.iyg16260.farmasterrae.dto.products;

import java.util.List;
import java.util.Map;

/**
 * Producto junto a su cantidad (carrito y detalles de pedido)
 *
 * @param product  producto
 * @param quantity cantidad
 */
public record ProductQuantityDTO(ProductDTO product, Integer quantity) {

    /**
     * Convierte un map producto-cantidad en un listado serializable a JSON
     *
     * @param products map de producto y cantidad
     * @return listado de productos con su cantidad
     */
    public static List<ProductQuantityDTO> fromMap(Map<ProductDTO, Integer> products) {
        if (products == null)
            return List.of();

        return products.entrySet().stream()
                .map(entry -> new ProductQuantityDTO(entry.getKey(), entry.getValue()))
                .toList();
    }
}
