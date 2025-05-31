package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.products.ProductPageDTO;
import com.iyg16260.farmasterrae.dto.user.ReviewDTO;
import com.iyg16260.farmasterrae.enums.Category;
import com.iyg16260.farmasterrae.service.ProductsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

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
    public ModelAndView getProducts(@RequestParam (required = false) Category category,
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

        return new ModelAndView("products/product-list")
                .addObject("products", products)
                .addObject("text", titlePage);
    }

    @GetMapping ("/{reference}")
    public ModelAndView getProductDetails(@PathVariable String reference,
                                          @RequestParam (defaultValue = "false", required = false) boolean added) {

        ProductDTO product = productsService.getProductDTOByReference(reference);
        List<ReviewDTO> reviewList = productsService.getReviewsFromProduct(reference);
        return new ModelAndView("products/product-details")
                .addObject("product", product)
                .addObject("reviews", reviewList)
                .addObject("added", added);
    }

}
