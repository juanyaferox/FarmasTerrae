package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.service.ProductsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@RequestMapping ("/products")
@Controller
public class ProductsController {

    @Autowired
    ProductsService productsService;

    @GetMapping ("/categories")
    public ModelAndView getCategories() {
        return new ModelAndView(("products/categories"));
    }

    @GetMapping
    public ModelAndView getProductList(@RequestParam (defaultValue = "0", required = false) int page) {
        return new ModelAndView("products/product-list")
                .addObject("products", productsService.getProductList(page));
    }

    @GetMapping ("/{reference}")
    public ModelAndView getProductDetails(@PathVariable String reference,
                                          @RequestParam (defaultValue = "false", required = false) boolean added) {

        return new ModelAndView("products/product-details")
                .addObject("product", productsService.getProductDTOByReference(reference))
                .addObject("added", added);
    }

}
