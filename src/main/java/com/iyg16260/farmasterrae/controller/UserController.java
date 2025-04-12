package com.iyg16260.farmasterrae.controller;

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
     * @param pageOrders pagina actual de la seccion de pedido, no obligatorio
     * @return dashboard + seccion solicitada, caso no coincida solo el dashboard
     */
    @GetMapping({"/dashboard/{section}", "/dashboard"})
    public ModelAndView changeSection(@PathVariable(required = false) String section, @AuthenticationPrincipal User user,
                                      @RequestParam(defaultValue = "0", required = false) int pageOrders) {

        System.out.println("prueba");
        ModelAndView model = new ModelAndView("user/dashboard")
                .addObject("section", section);

        if (section==null)
            return model;
        if (section.equals("info-user")) {
            UserDTO userDTO = new UserDTO();
            BeanUtils.copyProperties(user, userDTO);
            return model.addObject("userView", userDTO);
        }
        else if (section.equals("orders")) {
            Page<Order> orders = orderService.getOrders(user, pageOrders);
            return model.addObject("orders", orders);
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
    public void setInfoUser(@AuthenticationPrincipal User user, @ModelAttribute UserDTO userdto) {
        try {
            userService.updateUser(user.getId(), userdto);
        } catch (RuntimeException e) {
            log.error(e.getMessage());
        }
    }

    /**
     * Vista de detalles de un pedido con la informacion del pedido y una lista de sus productos
     * @param idOrder
     * @param user
     * @return
     */
    @GetMapping ("/dashboard/orders/{idOrder}")
    public ModelAndView getOrder(@PathVariable int idOrder, @AuthenticationPrincipal User user){
        return new ModelAndView("user/order-details")
                //.addObject("order", orderService.getOrder(user, idOrder))
                //.addObject("products", orderService.getProductsFromOrder(idOrder));
                .addObject("order",
                        orderService.getOrder(user.getId(), idOrder));
    }
}
