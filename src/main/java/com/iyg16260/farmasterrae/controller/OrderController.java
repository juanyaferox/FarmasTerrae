package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.payment.PaymentDetailsDTO;
import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.enums.PaymentMethod;
import com.iyg16260.farmasterrae.enums.SaleStatus;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.service.CartService;
import com.iyg16260.farmasterrae.service.OrderService;
import com.iyg16260.farmasterrae.utils.GenericUtils;
import com.iyg16260.farmasterrae.utils.SessionCart;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.util.WebUtils;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping ("/order")
public class OrderController {

    public static final String CHECKOUT_TOKEN_SESSION_KEY = "checkoutToken";

    @Autowired
    OrderService orderService;

    @Autowired
    CartService cartService;

    /**
     * Genera la vista de confirmación de pedido con token de idempotencia de un solo uso
     */
    @GetMapping
    public ModelAndView getOrder(HttpSession session, RedirectAttributes redirectAttributes,
                                 @AuthenticationPrincipal User user) {
        Map<ProductDTO, Integer> cartProducts;
        String checkoutToken;
        synchronized (WebUtils.getSessionMutex(session)) {
            SessionCart cart = cartService.getCart(session);
            if (cart.getProducts().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "El carrito está vacío");
                return new ModelAndView("redirect:/cart");
            }
            if (!cartService.validateCartReservations(session)) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Algunos productos no tienen suficiente stock " +
                                "o tu reserva ha expirado. Por favor, revisa tu carrito.");
                return new ModelAndView("redirect:/cart");
            }
            cartProducts = cartService.getDetailedProducts(session);
            checkoutToken = UUID.randomUUID().toString();
            session.setAttribute(CHECKOUT_TOKEN_SESSION_KEY, checkoutToken);
        }
        BigDecimal totalAmount = GenericUtils.priceAmountCalc(cartProducts);

        PaymentDetailsDTO paymentDetails = new PaymentDetailsDTO();
        paymentDetails.setAddress(user.getAddress());
        paymentDetails.setFullName(user.getFullName());

        return new ModelAndView("order/order-details")
                .addObject("products", cartProducts)
                .addObject("totalAmount", totalAmount)
                .addObject("paymentMethod", PaymentMethod.values())
                .addObject("paymentDetails", paymentDetails)
                .addObject("checkoutToken", checkoutToken);
    }

    /**
     * Valida y consume el token de idempotencia, crea el pedido y limpia el carrito
     */
    @PostMapping ("/confirm")
    public ModelAndView getPayment(@Valid @ModelAttribute PaymentDetailsDTO paymentDetails,
                                   @RequestParam (name = "checkoutToken", required = false) String checkoutToken,
                                   @AuthenticationPrincipal User user,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        Long orderId;
        synchronized (WebUtils.getSessionMutex(session)) {
            String expectedToken = (String) session.getAttribute(CHECKOUT_TOKEN_SESSION_KEY);
            boolean tokenValid = expectedToken != null && checkoutToken != null && expectedToken.equals(checkoutToken);
            if (!tokenValid) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Esta solicitud de pedido ya ha sido procesada o ha caducado. Revisa tus pedidos o tu carrito.");
                return new ModelAndView("redirect:/cart");
            }
            session.removeAttribute(CHECKOUT_TOKEN_SESSION_KEY);

            SessionCart cart = cartService.getCart(session);
            if (cart.getProducts().isEmpty()) {
                redirectAttributes.addFlashAttribute("errorMessage", "El carrito está vacío");
                return new ModelAndView("redirect:/cart");
            }

            if (!cartService.validateCartReservations(session)) {
                redirectAttributes.addFlashAttribute("errorMessage",
                        "Algunos productos no tienen suficiente stock o tu reserva ha expirado. Por favor, revisa tu carrito.");
                return new ModelAndView("redirect:/cart");
            }

            var order = orderService.setOrder(user, cart, SaleStatus.PENDING, paymentDetails, session);
            cartService.clearCart(session);
            orderId = order.getId();
        }

        redirectAttributes.addFlashAttribute("orderSuccess", true);
        return new ModelAndView("redirect:/user/dashboard/orders/" + orderId);
    }
}
