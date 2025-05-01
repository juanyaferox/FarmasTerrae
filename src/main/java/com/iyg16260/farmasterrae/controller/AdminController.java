package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.order.OrderDTO;
import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.user.UserDTO;
import com.iyg16260.farmasterrae.enums.SaleStatus;
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
@RequestMapping ("/admin")
public class AdminController {

    @Autowired
    OrderService orderService;

    @Autowired
    ProductsService productsService;

    @Autowired
    UserService userService;


    @GetMapping ({"/dashboard/{section}", "/dashboard"})
    public ModelAndView getDashboard(@PathVariable (required = false) String section,
                                     @AuthenticationPrincipal User user,
                                     @RequestParam (defaultValue = "0", required = false) int page) {

        ModelAndView model = new ModelAndView("admin/dashboard")
                .addObject("section", section);

        return processSection(section, model, user, page);
    }

    /**
     * Método_ para añadir secciones sin modificar el GetMapping, manteniendo el principio SOLID
     *
     * @param section seccion del pathvariable
     * @param model   model
     * @param user    user
     * @param page    page
     * @return vista de la seccion deseada
     */
    private ModelAndView processSection(String section, ModelAndView model,
                                        User user, int page) {
        if (section == null)
            return model;

        switch (section) {
            case "orders" -> {
                Page<OrderDTO> orders = orderService.getAllOrders(page);
                model.addObject("orders", orders);
                model.addObject("saleStatuses", SaleStatus.values());
            }
            case "products" -> {
                Page<ProductDTO> products = productsService.getProductList(page);
                model.addObject("products", products);
            }
            case "users" -> {
                Page<UserDTO> users = userService.getAllUsers(page);
                model.addObject("users", users);
            }
        }
        return model;
    }

}
