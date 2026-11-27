package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.service.CartService;
import com.iyg16260.farmasterrae.utils.GenericUtils;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
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

        ModelAndView model = new ModelAndView("cart/cart-view")
                .addObject("products", cart)
                .addObject("totalAmount", totalAmount);
        if (session.getAttribute(CartService.REMOVED_PRODUCT_NOTICE) != null) {
            model.addObject("errorMessage", "Algunos productos ya no están disponibles y se han eliminado del carrito.");
            session.removeAttribute(CartService.REMOVED_PRODUCT_NOTICE);
        }
        return model;
    }

    /**
     * Añadir producto al carrito
     */
    @PostMapping ("/add/{reference}")
    public String addToCart(@PathVariable String reference, HttpSession session, RedirectAttributes ra) {
        boolean added = cartService.addProductToCart(reference, session);
        String encodedReference = UriUtils.encodePathSegment(reference, StandardCharsets.UTF_8);
        if (added) {
            ra.addFlashAttribute("added", true);
        } else {
            ra.addFlashAttribute("errorMessage", "No hay stock disponible para añadir este producto.");
        }
        return "redirect:/products/" + encodedReference;
    }

    /**
     * Borrar todos los mismos de un producto del carrito
     */
    @PostMapping ("/remove/{reference}")
    public String deleteFromCart(@PathVariable String reference, HttpSession session) {
        cartService.deleteAllSameProductFromCart(reference, session);
        return CART_URL;
    }

    /**
     * Limpiar el carrito
     */
    @PostMapping ("/clear")
    public String clearCart(HttpSession session) {
        cartService.clearCart(session);
        return CART_URL;
    }

    /**
     * Añadir un producto del mismo al carrito
     */
    @PostMapping ("/increase/{reference}")
    public String increaseCart(@PathVariable String reference, HttpSession session,
                               RedirectAttributes ra) {
        if (!cartService.addProductToCart(reference, session)) {
            ra.addFlashAttribute("errorMessage", "No hay stock disponible para añadir más unidades de este producto.");
        }
        return CART_URL;
    }

    /**
     * Borrar un producto del mismo del carrito
     */
    @PostMapping ("/decrease/{reference}")
    public String decreaseCart(@PathVariable String reference, HttpSession session) {
        cartService.deleteProductFromCart(reference, session);
        return CART_URL;
    }

}
