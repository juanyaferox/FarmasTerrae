package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.payment.PaymentCardRequestDTO;
import com.iyg16260.farmasterrae.dto.payment.PaymentPaypalRequestDTO;
import com.iyg16260.farmasterrae.dto.payment.PaymentTransferenceRequestDTO;
import com.iyg16260.farmasterrae.enums.SaleEnum;
import com.iyg16260.farmasterrae.model.Order;
import com.iyg16260.farmasterrae.model.Product;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.service.CartService;
import com.iyg16260.farmasterrae.service.OrderService;
import com.iyg16260.farmasterrae.service.ProductsService;
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
                .addObject("products", cartService.getDetailedProducts(cart))
                .addObject("paymentMethod", SaleEnum.PaymentMethod.values());
    }

    @GetMapping ("/payment")
    public ModelAndView getPayment(@ModelAttribute SaleEnum.PaymentMethod paymentMethod) {
        return new ModelAndView("order/payment-gateway");
    }

    @PostMapping ("/payment")
    public ModelAndView setPayment(@ModelAttribute Object paymentDTO, @AuthenticationPrincipal User user, HttpSession session, RedirectAttributes redirectAttributes) {
        SessionCart cart = cartService.getCart(session);

        if (paymentDTO instanceof PaymentTransferenceRequestDTO)
            orderService.setOrder(user, cart, SaleEnum.SaleStatus.PENDING, SaleEnum.PaymentMethod.TRANSFER);
        if (paymentDTO instanceof PaymentPaypalRequestDTO)
            orderService.setOrder(user, cart, SaleEnum.SaleStatus.COMPLETED, SaleEnum.PaymentMethod.PAYPAL);
        if (paymentDTO instanceof PaymentCardRequestDTO)
            orderService.setOrder(user, cart, SaleEnum.SaleStatus.COMPLETED, SaleEnum.PaymentMethod.CREDIT_CARD);
        return new ModelAndView("redirect:/order/success");
    }



    @GetMapping("/success")
    public ModelAndView successOrder(@ModelAttribute Order order) {
        return new ModelAndView("/order/success")
                .addObject("order", order);
    }
}
