package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.service.ProductsService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@RequestMapping("/products")
@Controller
public class ProductsController {

    @Autowired
    ProductsService productsService;

    @GetMapping
    public ModelAndView getProductList(@RequestParam (defaultValue = "0", required = false) int page) {
        return new ModelAndView("products/product-list")
                .addObject("products", productsService.getProductList(page));
    }

    @GetMapping("/{idProduct}")
    public ModelAndView getProductDetails(@PathVariable long idProduct) {
        ProductDTO productDTO = new ProductDTO();
        BeanUtils.copyProperties(productsService.getProductById(idProduct), productDTO);

        return new ModelAndView("products/product-details")
                .addObject("product", productDTO);
    }

    @PostMapping("/{reference}")
    public String addToCart(@PathVariable String reference, HttpSession httpSession) {
        var product = productsService.getProductByReference(reference);
        return "redirect:/products/"+product.getId();
    }

}
