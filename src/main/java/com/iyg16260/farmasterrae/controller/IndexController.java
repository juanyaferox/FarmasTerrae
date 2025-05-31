package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.service.ProductsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Controller
@RequestMapping ("/")
public class IndexController {

    @Autowired
    private ProductsService productService;

    @GetMapping
    public String index(Model model) {
        // Obtener 3 productos aleatorios para la sección de productos destacados
        List<ProductDTO> featuredProducts = productService.getRandomProducts(4);

        // Agregar los productos al modelo para que estén disponibles en la vista
        model.addAttribute("featuredProducts", featuredProducts);

        return "index";
    }
}