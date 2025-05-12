package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.service.CartService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import java.util.Map;

@Controller
@RequestMapping ("/cart")
public class CartController {

    @Autowired
    CartService cartService;

    private final String CART_URL = "redirect:/cart";

    @GetMapping
    public ModelAndView viewCart(HttpSession session) {
        Map<ProductDTO, Integer> cart = cartService.getDetailedProducts(session);
        double totalAmount = 0.0;
        for (Map.Entry<ProductDTO, Integer> entry : cart.entrySet()) {
            totalAmount += entry.getKey().getPrice() * entry.getValue();
        }
        return new ModelAndView("cart/cart-view")
                .addObject("products", cart)
                .addObject("totalAmount", totalAmount);
    }

    @GetMapping ("/add/{reference}")
    public String addToCart(@PathVariable String reference, HttpSession session) {
        cartService.addProductToCart(reference, session);

        return "redirect:/products/" + reference + "?added=true";
    }

    @GetMapping ("/remove/{reference}")
    public String deleteFromCart(@PathVariable String reference,
                                 HttpSession session) {
        cartService.deleteAllSameProductFromCart(reference, session);
        return CART_URL;
    }

    @GetMapping ("/clear")
    public String clearCart(HttpSession session) {
        cartService.clearCart(session);
        return CART_URL;
    }

    @GetMapping ("/increase/{reference}")
    public String increaseCart(@PathVariable String reference, HttpSession session) {
        cartService.addProductToCart(reference, session);
        return CART_URL;
    }

    @GetMapping ("/decrease/{reference}")
    public String decreaseCart(@PathVariable String reference, HttpSession session) {
        cartService.deleteProductFromCart(reference, session);
        return CART_URL;
    }

    @GetMapping ("/checkout")
    public String goCheckout() {
        return "redirect:/checkout";
    }

}
