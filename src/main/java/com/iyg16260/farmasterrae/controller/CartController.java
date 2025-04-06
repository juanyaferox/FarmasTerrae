package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.model.Product;
import com.iyg16260.farmasterrae.service.CartService;
import com.iyg16260.farmasterrae.service.ProductsService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping ("/cart")
public class CartController {

    @Autowired
    CartService cartService;

    @Autowired
    ProductsService productsService;

    @GetMapping
    public ModelAndView viewCart (HttpSession session) {
        return new ModelAndView("cart/cart-view")
                .addObject("cart", cartService.getCart(session));
    }

    @PostMapping ("/add/{reference}")
    public String addToCart (@PathVariable String reference, HttpSession session) {
        Product product = productsService.getProductByReference(reference);
        cartService.addProductToCart(product, session);

        return "redirect:/products/" + product.getId() + "?added=true";
    }

    @PostMapping ("/delete/{reference}")
    public String deleteFromCart (@PathVariable String reference,
                                 @RequestParam (defaultValue = "false", required = false) boolean deleteAll,
                                 HttpSession session) {
        Product product = productsService.getProductByReference(reference);

        if (deleteAll == true)
            cartService.deleteAllSameProductFromCart(product.getId(), session);
        else
            cartService.deleteProductFromCart(product.getId(),session);

        return "redirect:/cart";
    }

    @PostMapping ("/clear")
    public String clearCart (HttpSession session) {
        cartService.clearCart(session);
        return "redirect:/cart";
    }
}
