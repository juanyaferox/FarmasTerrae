package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.order.OrderDTO;
import com.iyg16260.farmasterrae.enums.PaymentMethod;
import com.iyg16260.farmasterrae.enums.SaleStatus;
import com.iyg16260.farmasterrae.model.Order;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.service.CartService;
import com.iyg16260.farmasterrae.service.OrderService;
import com.iyg16260.farmasterrae.utils.GenericUtils;
import com.iyg16260.farmasterrae.utils.SessionCart;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@Slf4j
@RequestMapping ("/order")
public class OrderController {

    @Autowired
    OrderService orderService;

    @Autowired
    CartService cartService;

    /**
     * Genera una version inicial del pedido
     *
     * @param session
     * @param redirectAttributes
     * @return la vista con el listado de productos y un array paymentMethod
     */
    @GetMapping
    public ModelAndView getOrder(HttpSession session, RedirectAttributes redirectAttributes) {
        SessionCart cart = cartService.getCart(session);

        if (cart.getProducts() == null || cart.getProducts().isEmpty()) {
            redirectAttributes.addFlashAttribute("El carrito está vacío");
            return new ModelAndView("redirect:/cart");
        }
        return new ModelAndView("/order/order-details")
                .addObject("products", cartService.getDetailedProducts(session))
                .addObject("paymentMethod", PaymentMethod.values());
    }

    /**
     * Direcciona a la vista de pago con él metodo de pago
     *
     * @param paymentMethod
     * @return
     */
    @GetMapping ("/payment")
    public ModelAndView getPayment(@ModelAttribute PaymentMethod paymentMethod) {
        return new ModelAndView("order/payment-gateway");
    }

    /**
     * Realiza el pago
     *
     * @param paymentMethod      metodo de pago ya incluido en la vista
     * @param user
     * @param session
     * @param redirectAttributes
     * @return redirecciona al success o al carrito si por algo está vacio
     */
    @PostMapping ("/payment")
    public ModelAndView setPayment(@ModelAttribute PaymentMethod paymentMethod,
                                   @AuthenticationPrincipal User user, HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        SessionCart cart = cartService.getCart(session);

        if (cart.getProducts() == null || cart.getProducts().isEmpty()) {
            redirectAttributes.addFlashAttribute("El carrito está vacío");
            return new ModelAndView("redirect:/cart");
        }

        redirectAttributes.addFlashAttribute(
                orderService.setOrder(user, cart, SaleStatus.COMPLETED, paymentMethod)
        );
        return new ModelAndView("redirect:/order/success");
    }

    /**
     * Confirmación del pedido si fue exitoso
     *
     * @param order
     * @param session
     * @return vista con resumen del pedido
     */
    @GetMapping ("/success")
    public ModelAndView successOrder(@ModelAttribute Order order, HttpSession session) {
        cartService.getCart(session).clear();

        return new ModelAndView("/order/success")
                .addObject("order", GenericUtils.mapper(order, OrderDTO.class));
    }
}
