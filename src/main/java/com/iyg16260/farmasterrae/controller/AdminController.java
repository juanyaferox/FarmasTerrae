package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.order.OrderDTO;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.service.OrderService;
import com.iyg16260.farmasterrae.service.ProductsService;
import com.iyg16260.farmasterrae.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    OrderService orderService;

    @Autowired
    ProductsService productsService;

    @Autowired
    UserService userService;

    /**
     * TODO: Listado de usuarios con posibilidad de añadir, modificar y borrar
     * TODO: Listado de productos con posibilidad de añadir, modificar y borrar
     * TODO: Listado de pedidos con posibilidad de añadir, modificar y borrar
     */

    @GetMapping({"/dashboard/{section}", "/dashboard"})
    public ModelAndView getDashboard(@PathVariable (required = false) String section, @AuthenticationPrincipal User user,
                                     @RequestParam(defaultValue = "0", required = false) int pageOrders,
                                     @RequestParam(defaultValue = "0", required = false) int pageProducts,
                                     @RequestParam(defaultValue = "0", required = false) int pageUsers) {

        ModelAndView model = new ModelAndView("user/dashboard")
                .addObject("section", section);

        if (section==null)
            return model;

        if (section.equals("orders")) {
            Page<OrderDTO> orders = orderService.getOrders(user, pageOrders);
            return model.addObject("orders", orders);
        }

        if (section.equals("products")) {

        }
        if (section.equals("users")) {

        }
        return model;
    }
}
