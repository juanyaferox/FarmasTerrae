package com.iyg16260.farmasterrae.controller.admin;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.enums.EntityType;
import com.iyg16260.farmasterrae.enums.Operation;
import com.iyg16260.farmasterrae.service.ProductsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import static com.iyg16260.farmasterrae.utils.SuccessMessageUtils.buildSuccessMessage;

@Controller
@RequestMapping ("/admin/dashboard/products")
public class DashboardProductController {

    @Autowired
    ProductsService productsService;

    private final String PRODUCT_PATH = "redirect:/admin/dashboard/products";

    @PostMapping
    public String addProduct(@ModelAttribute ProductDTO productDTO, RedirectAttributes ra) {
        productsService.saveProduct(productDTO);
        buildSuccessMessage(ra, EntityType.PRODUCTS, Operation.POST);
        return PRODUCT_PATH;
    }

    @PutMapping
    public String updateProduct(@ModelAttribute ProductDTO productDTO, @RequestParam String oldReference, RedirectAttributes ra) {
        productsService.updateProduct(productDTO, oldReference);
        buildSuccessMessage(ra, EntityType.PRODUCTS, Operation.PUT);
        return PRODUCT_PATH;
    }

    @DeleteMapping
    public String deleteProduct(@RequestParam String reference, RedirectAttributes ra) {
        productsService.deleteProduct(reference);
        buildSuccessMessage(ra, EntityType.PRODUCTS, Operation.DELETE);
        return PRODUCT_PATH;
    }
}
