package com.iyg16260.farmasterrae.controller;

import com.iyg16260.farmasterrae.dto.order.OrderDTO;
import com.iyg16260.farmasterrae.dto.products.ProductDTO;
import com.iyg16260.farmasterrae.dto.user.OrderDetailsDTO;
import com.iyg16260.farmasterrae.dto.user.UserDTO;
import com.iyg16260.farmasterrae.enums.SaleEnum;
import com.iyg16260.farmasterrae.model.User;
import com.iyg16260.farmasterrae.service.OrderService;
import com.iyg16260.farmasterrae.service.ProductsService;
import com.iyg16260.farmasterrae.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping ("/admin")
public class AdminController {

    @Autowired
    OrderService orderService;

    @Autowired
    ProductsService productsService;

    @Autowired
    UserService userService;

    private final String ORDER_PATH = "redirect:/admin/dashboard/orders";
    private final String PRODUCT_PATH = "redirect:/admin/dashboard/products";
    private final String USER_PATH = "redirect:/admin/dashboard/users";

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
                model.addObject("saleStatuses", SaleEnum.SaleStatus.values());
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

    @GetMapping ("/dashboard/orders/{idOrder}")
    public ModelAndView getOrder(@PathVariable int idOrder, @AuthenticationPrincipal User user) {

        OrderDetailsDTO orderDetails = orderService.getOrder(user.getId(), idOrder);

        return new ModelAndView("user/order-details")
                .addObject("order", orderDetails);
    }

    @PutMapping ("/dashboard/orders")
    public String updateOrder(@RequestParam long idOrder, @RequestParam String status, RedirectAttributes ra) {
        orderService.updateOrder(idOrder, status);
        buildSuccessMessage(ra, "products", "put");
        return ORDER_PATH;
    }

    @DeleteMapping ("/dashboard/orders")
    public String deleteOrder(@RequestParam long idOrder, RedirectAttributes ra) {
        orderService.deleteOrderById(idOrder);
        buildSuccessMessage(ra, "products", "delete");
        return ORDER_PATH;
    }

    @PostMapping ("/dashboard/products")
    public String addProduct(@ModelAttribute ProductDTO productDTO, RedirectAttributes ra) {
        productsService.saveProduct(productDTO);
        buildSuccessMessage(ra, "products", "post");
        return PRODUCT_PATH;
    }

    @PutMapping ("/dashboard/products")
    public String updateProduct(@ModelAttribute ProductDTO productDTO, @RequestParam String oldReference, RedirectAttributes ra) {
        productsService.updateProduct(productDTO, oldReference);
        buildSuccessMessage(ra, "products", "put");
        return PRODUCT_PATH;
    }

    @DeleteMapping ("/dashboard/products")
    public String deleteProduct(@RequestParam String reference, RedirectAttributes ra) {
        productsService.deleteProduct(reference);
        buildSuccessMessage(ra, "products", "delete");
        return PRODUCT_PATH;
    }

    private void buildSuccessMessage(RedirectAttributes ra, String type, String operation) {
        switch (type) {
            case "products" -> ra.addFlashAttribute
                    ("successMessage", auxiliarForSuccessMessage("Producto", operation));
            case "orders" -> ra.addFlashAttribute
                    ("successMessage", auxiliarForSuccessMessage("Pedido", operation));
            case "users" -> ra.addFlashAttribute
                    ("successMessage", auxiliarForSuccessMessage("Usuario", operation));
        }
    }

    private String auxiliarForSuccessMessage(String type, String operation) {
        switch (operation) {
            case "post" -> {
                return type + " añadido con éxito.";
            }
            case "delete" -> {
                return type + " eliminado con éxito.";
            }
            case "put" -> {
                return type + " actualizado con éxito.";
            }
            default -> {
                return "Operación en " + type + " realiza con éxito";
            }
        }
    }
}
