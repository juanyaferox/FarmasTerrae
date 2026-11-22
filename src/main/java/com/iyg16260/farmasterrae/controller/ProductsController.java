package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.products.ProductPageDTO;
import com.iyg16260.farmasterrae.dto.response.EnumOptionDTO;
import com.iyg16260.farmasterrae.dto.response.ProductDetailsResponseDTO;
import com.iyg16260.farmasterrae.dto.response.ProductListResponseDTO;
import com.iyg16260.farmasterrae.dto.user.ReviewDTO;
import com.iyg16260.farmasterrae.enums.Category;
import com.iyg16260.farmasterrae.service.ProductsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;

import static com.iyg16260.farmasterrae.config.OpenApiConfig.BAD_REQUEST;
import static com.iyg16260.farmasterrae.config.OpenApiConfig.TAG_PRODUCTS;

@Tag (name = TAG_PRODUCTS)
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
    @Operation (summary = "Listar categorías",
            description = "Categorías de producto con su código (para filtrar) y su nombre legible")
    @ApiResponse (responseCode = "200", description = "Listado de categorías")
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
    @Operation (summary = "Listar productos",
            description = "Listado paginado de productos, filtrado opcionalmente por categoría o texto. " +
                    "Incluye el título del listado (categoría, búsqueda o todos los productos)")
    @ApiResponse (responseCode = "200", description = "Página de productos")
    @ApiResponse (responseCode = "400", ref = BAD_REQUEST)
    @GetMapping
    public ProductListResponseDTO getProducts(@Parameter (description = "Categoría por la que filtrar")
                                              @RequestParam (required = false) Category category,
                                              @Parameter (description = "Número de página, empieza en 0")
                                              @RequestParam (defaultValue = "0", required = false) int page,
                                              @Parameter (description = "Texto a buscar")
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
    @Operation (summary = "Detalle de un producto", description = "Producto con todas sus reseñas")
    @ApiResponse (responseCode = "200", description = "Producto y reseñas")
    @GetMapping ("/{reference}")
    public ProductDetailsResponseDTO getProductDetails(@Parameter (description = "Referencia del producto", example = "REFMED001")
                                                       @PathVariable String reference) {
        ProductDTO product = productsService.getProductDTOByReference(reference);
        List<ReviewDTO> reviewList = productsService.getReviewsFromProduct(reference);
        return new ProductDetailsResponseDTO(product, reviewList);
    }

}
