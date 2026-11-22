package com.iyg16260.farmasterrae.controller.admin;

import com.iyg16260.farmasterrae.dto.response.MessageResponseDTO;
import com.iyg16260.farmasterrae.dto.user.OrderDetailsDTO;
import com.iyg16260.farmasterrae.enums.EntityType;
import com.iyg16260.farmasterrae.enums.Operation;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.service.OrderService;
import com.iyg16260.farmasterrae.utils.SuccessMessageUtils;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static com.iyg16260.farmasterrae.config.OpenApiConfig.*;

@Tag (name = TAG_ADMIN_ORDERS)
@SecurityRequirement (name = SESSION_COOKIE)
@ApiResponse (responseCode = "401", ref = UNAUTHORIZED)
@ApiResponse (responseCode = "403", ref = FORBIDDEN)
@RestController
@RequestMapping ("/api/admin/orders")
public class DashboardOrderController {

    @Autowired
    OrderService orderService;

    @io.swagger.v3.oas.annotations.Operation (summary = "Detalle de un pedido",
            description = "Información de cualquier pedido y sus productos")
    @ApiResponse (responseCode = "200", description = "Detalle del pedido")
    @ApiResponse (responseCode = "404", ref = NOT_FOUND)
    @GetMapping ("/{idOrder}")
    public OrderDetailsDTO getOrder(@Parameter (description = "Id del pedido") @PathVariable int idOrder,
                                    @AuthenticationPrincipal User user) {
        return orderService.getOrder(user.getId(), idOrder);
    }

    /**
     * Cambia el estado del pedido
     *
     * @param idOrder id del pedido
     * @param status  nuevo estado (nombre del enum SaleStatus)
     */
    @io.swagger.v3.oas.annotations.Operation (summary = "Cambiar el estado de un pedido")
    @ApiResponse (responseCode = "200", description = "Pedido actualizado")
    @ApiResponse (responseCode = "400", ref = BAD_REQUEST)
    @ApiResponse (responseCode = "404", ref = NOT_FOUND)
    @PutMapping ("/{idOrder}")
    public MessageResponseDTO updateOrder(@Parameter (description = "Id del pedido") @PathVariable long idOrder,
                                          @Parameter (description = "Nuevo estado (código de SaleStatus)",
                                                  example = "SHIPPED")
                                          @RequestParam String status) {
        orderService.updateOrder(idOrder, status);
        return SuccessMessageUtils.buildSuccessMessage(EntityType.ORDERS, Operation.PUT);
    }

    @io.swagger.v3.oas.annotations.Operation (summary = "Eliminar un pedido")
    @ApiResponse (responseCode = "200", description = "Pedido eliminado")
    @ApiResponse (responseCode = "404", ref = NOT_FOUND)
    @DeleteMapping ("/{idOrder}")
    public MessageResponseDTO deleteOrder(@Parameter (description = "Id del pedido") @PathVariable long idOrder) {
        orderService.deleteOrderById(idOrder);
        return SuccessMessageUtils.buildSuccessMessage(EntityType.ORDERS, Operation.DELETE);
    }
}
