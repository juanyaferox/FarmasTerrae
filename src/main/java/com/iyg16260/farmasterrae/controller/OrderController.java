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
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.Map;

@Controller
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
    public ModelAndView getOrder(HttpSession session, RedirectAttributes redirectAttributes,
                                 @AuthenticationPrincipal User user) {
        SessionCart cart = cartService.getCart(session);
        if (cart.getProducts() == null || cart.getProducts().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "El carrito está vacío");
            return new ModelAndView("redirect:/cart");
        }
        // Verificar que las reservas de stock son válidas
        if (!cartService.validateCartReservations(session)) {
            // Si no son válidas, redirigir al carrito con un mensaje de error
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Algunos productos no tienen suficiente stock " +
                            "o tu reserva ha expirado. Por favor, revisa tu carrito.");
            return new ModelAndView("redirect:/cart");
        }
        Map<ProductDTO, Integer> cartProducts = cartService.getDetailedProducts(session);
        BigDecimal totalAmount = GenericUtils.priceAmountCalc(cartProducts);

        PaymentDetailsDTO paymentDetails = new PaymentDetailsDTO();
        paymentDetails.setAmount(totalAmount);
        paymentDetails.setAddress(user.getAddress());
        paymentDetails.setFullName(user.getFullName());

        return new ModelAndView("order/order-details")
                .addObject("products", cartProducts)
                .addObject("paymentMethod", PaymentMethod.values())
                .addObject("paymentDetails", paymentDetails);
    }

    /**
     * Direcciona a la vista de pago con él metodo de pago
     *
     * @param paymentDetails
     * @return
     */
    @PostMapping ("/confirm")
    public ModelAndView getPayment(@Valid @ModelAttribute PaymentDetailsDTO paymentDetails,
                                   @AuthenticationPrincipal User user,
                                   HttpSession session,
                                   RedirectAttributes redirectAttributes) {

        // Verificar nuevamente las reservas de stock
        if (!cartService.validateCartReservations(session)) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Algunos productos no tienen suficiente stock o tu reserva ha expirado. Por favor, revisa tu carrito.");
            return new ModelAndView("redirect:/cart");
        }

        SessionCart cart = cartService.getCart(session);

        if (cart.getProducts() == null || cart.getProducts().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "El carrito está vacío");
            return new ModelAndView("redirect:/cart");
        }

        var order = orderService.setOrder(user, cart, SaleStatus.PENDING, paymentDetails, session);
        // Intentar crear el pedido (esto verificará y confirmará las reservas de stock)
        redirectAttributes.addFlashAttribute(order);

        return new ModelAndView("redirect:/user/dashboard/orders/" + order.getId() + "?success=true");
    }

    // SIN USAR, ENFOQUE ORIGINAL PARA TENER UN PAGO MAS DETALLADO
    /*
    @PostMapping("/payment")
    public ModelAndView setPayment(@ModelAttribute PaymentMethod paymentMethod,
                                   @AuthenticationPrincipal User user, HttpSession session,
                                   RedirectAttributes redirectAttributes) {
        SessionCart cart = cartService.getCart(session);

        if (cart.getProducts() == null || cart.getProducts().isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "El carrito está vacío");
            return new ModelAndView("redirect:/cart");
        }
            // Intentar crear el pedido (esto verificará y confirmará las reservas de stock)
            redirectAttributes.addFlashAttribute(
                    orderService.setOrder(user, cart, SaleStatus.COMPLETED, paymentMethod, session)
            );
            return new ModelAndView("redirect:/order/success");
    }
    @GetMapping ("/success")
    public ModelAndView successOrder(@ModelAttribute Order order, HttpSession session) {
        cartService.clearCart(session);

        return new ModelAndView("order/success")
                .addObject("order", order);
    }
     */
}