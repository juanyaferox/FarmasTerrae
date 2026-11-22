package com.iyg16260.farmasterrae.dto.response;

import com.iyg16260.farmasterrae.dto.products.ProductPageDTO;
import org.springframework.data.web.PagedModel;

/**
 * Listado paginado de productos
 *
 * @param title    título del listado (categoría, búsqueda o todos)
 * @param products página de productos
 */
public record ProductListResponseDTO(String title, PagedModel<ProductPageDTO> products) {
}
