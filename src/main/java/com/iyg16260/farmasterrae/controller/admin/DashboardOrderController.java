package com.iyg16260.farmasterrae.controller.admin;

import com.iyg16260.farmasterrae.dto.user.OrderDetailsDTO;
import com.iyg16260.farmasterrae.enums.EntityType;
import com.iyg16260.farmasterrae.enums.Operation;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.service.OrderService;
import com.iyg16260.farmasterrae.utils.SuccessMessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping ("/admin/dashboard/orders")
public class DashboardOrderController {

    @Autowired
    OrderService orderService;


    private final String ORDER_PATH = "redirect:/admin/dashboard/orders";

    @GetMapping ("/{idOrder}")
    public ModelAndView getOrder(@PathVariable int idOrder, @AuthenticationPrincipal User user) {
        OrderDetailsDTO orderDetails = orderService.getOrder(user.getId(), idOrder);
        return new ModelAndView("admin/order-details")
                .addObject("order", orderDetails);
    }

    @PutMapping
    public String updateOrder(@RequestParam long idOrder, @RequestParam String status, RedirectAttributes ra) {
        orderService.updateOrder(idOrder, status);
        SuccessMessageUtils.buildSuccessMessage(ra, EntityType.ORDERS, Operation.PUT);
        return ORDER_PATH;
    }

    @DeleteMapping
    public String deleteOrder(@RequestParam long idOrder, RedirectAttributes ra) {
        orderService.deleteOrderById(idOrder);
        SuccessMessageUtils.buildSuccessMessage(ra, EntityType.ORDERS, Operation.DELETE);
        return ORDER_PATH;
    }
}
