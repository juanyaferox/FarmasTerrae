package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.model.Product;
import com.iyg16260.farmasterrae.service.CartService;
import com.iyg16260.farmasterrae.service.ProductsService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping ("/cart")
public class CartController {

    @Autowired
    CartService cartService;

    @Autowired
    ProductsService productsService;

    @PostMapping ("/add/{reference}")
    public String addToCart(@PathVariable String reference, HttpSession session) {
        Product product = productsService.getProductByReference(reference);
        cartService.addProductToCart(product, session);

        return "redirect:/products/" + product.getId() + "?added=true";
    }

    @PostMapping ("/delete/{reference}")
    public String deleteFromCart(@PathVariable String reference,
                                 @RequestParam (defaultValue = "false", required = false) boolean deleteAll,
                                 HttpSession session) {
        Product product = productsService.getProductByReference(reference);

        if (deleteAll == true)
            cartService.deleteAllSameProductFromCart(product.getId(), session);
        else
            cartService.deleteProductFromCart(product.getId(),session);

        return "redirect:/cart/view";
    }
}
