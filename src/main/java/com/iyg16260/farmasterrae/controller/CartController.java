package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.service.CartService;
import com.iyg16260.farmasterrae.utils.GenericUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Controller
@RequestMapping ("/cart")
public class CartController {

    @Autowired
    CartService cartService;

    private final String CART_URL = "redirect:/cart";

    /**
     * Acceder al carrito
     */
    @GetMapping
    public ModelAndView viewCart(HttpSession session) {
        Map<ProductDTO, Integer> cart = cartService.getDetailedProducts(session);

        var totalAmount = GenericUtils.priceAmountCalc(cart);

        return new ModelAndView("cart/cart-view")
                .addObject("products", cart)
                .addObject("totalAmount", totalAmount);
    }

    /**
     * Añadir producto al carrito
     */
    @GetMapping ("/add/{reference}")
    public String addToCart(@PathVariable String reference, HttpSession session, RedirectAttributes ra) {
        cartService.addProductToCart(reference, session);

        return "redirect:/products/" + reference + "?added=true";
    }

    /**
     * Borrar todos los mismos de un producto del carrito
     *
     * @return
     */
    @GetMapping ("/remove/{reference}")
    public String deleteFromCart(@PathVariable String reference,
                                 HttpSession session) {
        cartService.deleteAllSameProductFromCart(reference, session);
        return CART_URL;
    }

    /**
     * Limpiar el carrito
     */
    @GetMapping ("/clear")
    public String clearCart(HttpSession session) {
        cartService.clearCart(session);
        return CART_URL;
    }

    /**
     * Añadir un producto del mismo al carrito
     */
    @GetMapping ("/increase/{reference}")
    public String increaseCart(@PathVariable String reference, HttpSession session, RedirectAttributes ra) {
        cartService.addProductToCart(reference, session);
        return CART_URL;
    }

    /**
     * Borrar un producto del mismo del carrito
     */
    @GetMapping ("/decrease/{reference}")
    public String decreaseCart(@PathVariable String reference, HttpSession session) {
        cartService.deleteProductFromCart(reference, session);
        return CART_URL;
    }

}
