package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.response.HomeResponseDTO;
import com.iyg16260.farmasterrae.service.ProductsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static com.iyg16260.farmasterrae.config.OpenApiConfig.TAG_HOME;

@Tag (name = TAG_HOME)
@RestController
@RequestMapping ("/api")
public class IndexController {

    @Autowired
    private ProductsService productService;

    /**
     * Datos de la página de inicio
     *
     * @return productos destacados
     */
    @Operation (summary = "Datos de la página de inicio",
            description = "Devuelve 4 productos destacados elegidos al azar")
    @ApiResponse (responseCode = "200", description = "Productos destacados")
    @GetMapping ("/home")
    public HomeResponseDTO index() {
        List<ProductDTO> featuredProducts = productService.getRandomProducts(4);
        return new HomeResponseDTO(featuredProducts);
    }
}
