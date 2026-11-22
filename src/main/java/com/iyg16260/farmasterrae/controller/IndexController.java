package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.response.HomeResponseDTO;
import com.iyg16260.farmasterrae.service.ProductsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

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
    @GetMapping ("/home")
    public HomeResponseDTO index() {
        List<ProductDTO> featuredProducts = productService.getRandomProducts(4);
        return new HomeResponseDTO(featuredProducts);
    }
}
