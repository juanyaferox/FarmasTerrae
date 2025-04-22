package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.order.OrderDTO;
import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.user.OrderDetailsDTO;
import com.iyg16260.farmasterrae.dto.user.UserDTO;
import com.iyg16260.farmasterrae.model.Order;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.service.OrderService;
import com.iyg16260.farmasterrae.service.UserService;
import com.iyg16260.farmasterrae.utils.GenericUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@Slf4j
@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    UserService userService;

    @Autowired
    OrderService orderService;

    /**
     * Para acceder al dashboard y cambiar entre secciones
     * @param section nombre de la seccion a validar en el if ternario de la vista, no obligatorio
     * @param user
     * @param page pagina actual de la seccion de pedido, no obligatorio
     * @return dashboard + seccion solicitada, caso no coincida solo el dashboard
     */
    @GetMapping({"/dashboard/{section}", "/dashboard"})
    public ModelAndView changeSection(@PathVariable(required = false) String section, @AuthenticationPrincipal User user,
                                      @RequestParam(defaultValue = "0", required = false) int page) {

        ModelAndView model = new ModelAndView("user/dashboard")
                .addObject("section", section);

        return processSection(section, model, user, page);
    }

    /**
     * Método_ para añadir secciones sin modificar el GetMapping, manteniendo el principio SOLID
     * @param section seccion del pathvariable
     * @param model model
     * @param user user
     * @param page page
     * @return vista de la seccion deseada
     */
    private ModelAndView processSection(String section, ModelAndView model,
                                        User user, int page) {
        if (section == null)
            return model;

        switch (section) {
            case "orders" -> {
                Page<OrderDTO> orders = orderService.getOrders(user, page);
                model.addObject("orders", orders);
            }
            case "info-user" -> {
                UserDTO userDTO = userService.getUserById(user.getId());
                return model.addObject("userView", userDTO);
            }
        }

        return model;
    }

    /**
     * Cambia la informacion del usuario recibiendo el userDto
     * @param user
     * @param userdto
     * @return redirecciona al dashboard
     */
    @PostMapping("/dashboard/info-user")
    public ModelAndView setInfoUser(@AuthenticationPrincipal User user, @ModelAttribute("userView") UserDTO userdto) {
        try {
            userService.updateUser(user.getId(), userdto);
        } catch (RuntimeException e) {
            log.error(e.getMessage());
        }
        return new ModelAndView("redirect:/user/dashboard/info-user");
    }

    /**
     * Vista de detalles de un pedido con la informacion del pedido y una lista de sus productos
     * @param idOrder
     * @param user
     * @return
     */
    @GetMapping ("/dashboard/orders/{idOrder}")
    public ModelAndView getOrder(@PathVariable int idOrder, @AuthenticationPrincipal User user){

        OrderDetailsDTO orderDetails = orderService.getOrder(user.getId(), idOrder);

        return new ModelAndView("user/order-details")
                .addObject("order", orderDetails);
    }
}
