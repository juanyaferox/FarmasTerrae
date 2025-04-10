package com.iyg16260.farmasterrae.controller;

import ch.qos.logback.core.joran.util.beans.BeanUtil;
import com.iyg16260.farmasterrae.dto.order.OrderDTO;
import com.iyg16260.farmasterrae.dto.payment.PaymentCardRequestDTO;
import com.iyg16260.farmasterrae.enums.SaleEnum;
import com.iyg16260.farmasterrae.model.Order;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.service.CartService;
import com.iyg16260.farmasterrae.service.OrderService;
import com.iyg16260.farmasterrae.utils.GenericUtils;
import com.iyg16260.farmasterrae.utils.SessionCart;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.jaxb.SpringDataJaxb;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
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

    @GetMapping
    public ModelAndView getOrder(HttpSession session, RedirectAttributes redirectAttributes) {
        SessionCart cart = cartService.getCart(session);

        if (cart.getProducts() == null || cart.getProducts().isEmpty()) {
            redirectAttributes.addFlashAttribute("El carrito está vacío");
            return new ModelAndView("redirect:/cart");
        }
        return new ModelAndView("/order/order-details")
                        .addObject("products", cartService.getDetailedProducts(session))
                        .addObject("paymentMethod", SaleEnum.PaymentMethod.values());
    }

    @GetMapping ("/payment")
    public ModelAndView getPayment(@ModelAttribute SaleEnum.PaymentMethod paymentMethod) {
        return new ModelAndView("order/payment-gateway");
    }

    @PostMapping ("/payment")
    public ModelAndView setPayment(@ModelAttribute SaleEnum.PaymentMethod paymentMethod,
                                   @AuthenticationPrincipal User user, HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        SessionCart cart = cartService.getCart(session);

        if (cart.getProducts() == null || cart.getProducts().isEmpty()) {
            redirectAttributes.addFlashAttribute("El carrito está vacío");
            return new ModelAndView("redirect:/cart");
        }

        orderService.setOrder(user, cart, SaleEnum.SaleStatus.COMPLETED, paymentMethod);
        return new ModelAndView("redirect:/order/success");
    }



    @GetMapping("/success")
    public ModelAndView successOrder(@ModelAttribute Order order, HttpSession session) {
        cartService.getCart(session).clear();

        return new ModelAndView("/order/success")
                .addObject("order", GenericUtils.mapper(order, OrderDTO.class));
    }
}
