package com.iyg16260.farmasterrae.controller.admin;

import com.iyg16260.farmasterrae.dto.response.MessageResponseDTO;
import com.iyg16260.farmasterrae.dto.user.OrderDetailsDTO;
import com.iyg16260.farmasterrae.enums.EntityType;
import com.iyg16260.farmasterrae.enums.Operation;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.service.OrderService;
import com.iyg16260.farmasterrae.utils.SuccessMessageUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping ("/api/admin/orders")
public class DashboardOrderController {

    @Autowired
    OrderService orderService;

    @GetMapping ("/{idOrder}")
    public OrderDetailsDTO getOrder(@PathVariable int idOrder, @AuthenticationPrincipal User user) {
        return orderService.getOrder(user.getId(), idOrder);
    }

    /**
     * Cambia el estado del pedido
     *
     * @param idOrder id del pedido
     * @param status  nuevo estado (nombre del enum SaleStatus)
     */
    @PutMapping ("/{idOrder}")
    public MessageResponseDTO updateOrder(@PathVariable long idOrder, @RequestParam String status) {
        orderService.updateOrder(idOrder, status);
        return SuccessMessageUtils.buildSuccessMessage(EntityType.ORDERS, Operation.PUT);
    }

    @DeleteMapping ("/{idOrder}")
    public MessageResponseDTO deleteOrder(@PathVariable long idOrder) {
        orderService.deleteOrderById(idOrder);
        return SuccessMessageUtils.buildSuccessMessage(EntityType.ORDERS, Operation.DELETE);
    }
}
