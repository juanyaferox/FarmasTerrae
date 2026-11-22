package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.products.ProductPageDTO;
import com.iyg16260.farmasterrae.dto.response.EnumOptionDTO;
import com.iyg16260.farmasterrae.dto.response.ProductDetailsResponseDTO;
import com.iyg16260.farmasterrae.dto.response.ProductListResponseDTO;
import com.iyg16260.farmasterrae.dto.user.ReviewDTO;
import com.iyg16260.farmasterrae.enums.Category;
import com.iyg16260.farmasterrae.service.ProductsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping ("/api/products")
public class ProductsController {

    @Autowired
    ProductsService productsService;

    /**
     * Listado de categorías disponibles
     *
     * @return categorías con su código y etiqueta
     */
    @GetMapping ("/categories")
    public List<EnumOptionDTO> getCategories() {
        return Arrays.stream(Category.values())
                .map(category -> new EnumOptionDTO(category.name(), category.getValue()))
                .toList();
    }

    /**
     * Listado paginado de productos filtrado por categoría o búsqueda
     *
     * @param category categoría, no obligatoria
     * @param page     página actual, no obligatoria
     * @param search   búsqueda, no obligatoria
     * @return título del listado y página de productos
     */
    @GetMapping
    public ProductListResponseDTO getProducts(@RequestParam (required = false) Category category,
                                              @RequestParam (defaultValue = "0", required = false) int page,
                                              @RequestParam (required = false) String search) {
        Page<ProductPageDTO> products = productsService.getFilteredProducts(category, search, page);
        String titlePage;
        if (category != null)
            titlePage = category.getValue();
        else if (search != null && !search.isBlank())
            titlePage = "Resultados para: " + search;
        else
            titlePage = "Todos los Productos";

        return new ProductListResponseDTO(titlePage, new PagedModel<>(products));
    }

    /**
     * Detalle de un producto con sus reseñas
     *
     * @param reference referencia del producto
     * @return producto y reseñas
     */
    @GetMapping ("/{reference}")
    public ProductDetailsResponseDTO getProductDetails(@PathVariable String reference) {
        ProductDTO product = productsService.getProductDTOByReference(reference);
        List<ReviewDTO> reviewList = productsService.getReviewsFromProduct(reference);
        return new ProductDetailsResponseDTO(product, reviewList);
    }

}
