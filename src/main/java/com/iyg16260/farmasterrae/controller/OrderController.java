package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.model.Product;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.service.CartService;
import com.iyg16260.farmasterrae.service.OrderService;
import com.iyg16260.farmasterrae.utils.SessionCart;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@Slf4j
@RequestMapping ("/order")
public class OrderController {

    @Autowired
    OrderService orderService;

    @Autowired
    CartService cartService;

    @GetMapping
    public ModelAndView getOrder(HttpSession session, RedirectAttributes redirectAttributes) {
        SessionCart cart = cartService.getCart(session);

        if (cart.getProducts() == null || cart.getProducts().isEmpty()) {
            redirectAttributes.addFlashAttribute("El carrito está vacío");
            return new ModelAndView("redirect:/cart");
        }

        return new ModelAndView("/order/order-details")
                .addObject("products", cart.getProducts());
    }

    @PostMapping ("/confirm")
    public ModelAndView confirmOrder(HttpSession session, RedirectAttributes redirectAttributes) {
        SessionCart cart = cartService.getCart(session);
        List<Product> productsToOrder = cart.getProducts();

        if (cart.getProducts() == null || cart.getProducts().isEmpty()) {
            redirectAttributes.addFlashAttribute("El carrito está vacío");
            return new ModelAndView("redirect:/order");
        }
        // añadir logica del carrito, caso algo resulte mal por algun motivo throwear un error.
        // recordar que al hacer llamada, resta 1 de stock.
        return new ModelAndView();
    }

    @GetMapping("/success")
    public ModelAndView successOrder() {
        return new ModelAndView();
    }
}
