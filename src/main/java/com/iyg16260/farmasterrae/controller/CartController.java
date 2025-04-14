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
    
    @GetMapping
    public ModelAndView viewCart (HttpSession session) {
        return new ModelAndView("cart/cart-view")
                .addObject("cart", cartService.getCart(session));
    }

    @PostMapping ("/add/{reference}")
    public String addToCart (@PathVariable String reference, HttpSession session) {
        cartService.addProductToCart(reference, session);

        return "redirect:/products/" + reference + "?added=true";
    }

    @PostMapping ("/delete/{reference}")
    public String deleteFromCart (@PathVariable String reference,
                                 @RequestParam (defaultValue = "false", required = false) boolean deleteAll,
                                 HttpSession session) {
        if (deleteAll)
            cartService.deleteAllSameProductFromCart(reference, session);
        else
            cartService.deleteProductFromCart(reference,session);

        return "redirect:/cart";
    }

    @PostMapping ("/clear")
    public String clearCart (HttpSession session) {
        cartService.clearCart(session);
        return "redirect:/cart";
    }
}
