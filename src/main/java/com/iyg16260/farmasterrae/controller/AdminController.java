package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.order.OrderDTO;
import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.user.UserDTO;
import com.iyg16260.farmasterrae.enums.Category;
import com.iyg16260.farmasterrae.enums.SaleStatus;
import com.iyg16260.farmasterrae.model.Profile;
import com.iyg16260.farmasterrae.service.OrderService;
import com.iyg16260.farmasterrae.service.ProductsService;
import com.iyg16260.farmasterrae.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

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
                                     @RequestParam (defaultValue = "0", required = false) int page,
                                     @RequestParam (required = false) String sort,
                                     @RequestParam (defaultValue = "asc") String dir,
                                     @RequestParam (required = false) String search) {

        ModelAndView model = new ModelAndView("admin/dashboard")
                .addObject("section", section);

        return processSection(section, model, search, sort, dir, page);
    }

    /**
     * Método_ para añadir secciones sin modificar el GetMapping, manteniendo el principio SOLID
     *
     * @param section seccion del pathvariable
     * @param model   model
     * @param search  búsqueda
     * @param page    page
     * @return vista de la seccion deseada
     */
    private ModelAndView processSection(String section, ModelAndView model, String search, String sort, String dir, int page) {
        if (section == null)
            return model;

        switch (section) {
            case "orders" -> {
                Page<OrderDTO> orders = orderService.getAllOrders(page, search, sort, dir);
                model.addObject("orders", orders);
                model.addObject("saleStatuses", SaleStatus.values());
            }
            case "products" -> {
                Page<ProductDTO> products = productsService.getProductList(page, search, sort, dir);
                model.addObject("products", products);
                model.addObject("categories", Category.values());
            }
            case "users" -> {
                Page<UserDTO> users = userService.getAllUsers(page, search, sort, dir);
                List<Profile> profiles = userService.getProfiles();
                model.addObject("users", users);
                model.addObject("profiles", profiles);
            }
        }
        return model;
    }
}
